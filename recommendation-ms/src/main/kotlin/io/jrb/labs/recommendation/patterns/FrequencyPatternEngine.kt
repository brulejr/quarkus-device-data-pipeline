/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.recommendation.patterns

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.datatypes.Rtl433Data
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.recommendation.model.RecommendationEntity
import io.jrb.labs.recommendation.repository.RecommendationRepo
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@ApplicationScoped
class FrequencyPatternEngine @Inject constructor(
    private val recommendationRepo: RecommendationRepo
) : PatternEngine {

    private val log by LoggerDelegate()

    // Sliding counters (in-memory). You can swap to Redis later.
    private val counts = ConcurrentHashMap<String, AtomicLong>()
    private val firstSeen = ConcurrentHashMap<String, Instant>()
    private val lastSeen = ConcurrentHashMap<String, Instant>()

    // Tunables
    private val minObservations = 3L           // require at least N hits
    private val minLifespan = Duration.ofMinutes(3) // seen over at least this timespan
    private val cooldown = Duration.ofMinutes(30)    // avoid re-emitting too often

    override fun learn(msg: Rtl433Message) {
        val data: Rtl433Data = msg.payload
        val fp = Fingerprints.structureFingerprint(data.model, data.getProperties())
        val key = fp.key

        val now = Instant.now()
        counts.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
        firstSeen.putIfAbsent(key, now)
        lastSeen[key] = now

        maybeRecommend(key, data, fp, now)
    }

    private fun maybeRecommend(key: String, data: Rtl433Data, fp: PayloadFingerprint, now: Instant) {
        val c = counts[key]?.get() ?: return
        val age = Duration.between(firstSeen[key] ?: now, now)
        val last = lastSeen[key] ?: now
        log.info("learning - key = $key, fp = $fp, c = $c, age = $age, data = $data")

        // Already recommended recently?
        val existing = recommendationRepo.findByFingerprint(fp.model, fp.structureHash)
        if (existing != null && Duration.between(existing.lastEmittedAt, now) < cooldown) return

        if (c >= minObservations && age >= minLifespan) {
            val rec = RecommendationEntity.from(
                model = data.model,
                fingerprint = fp.structureHash,
                examples = 1,
                score = score(c, age),
                lastEmittedAt = now
            )
            recommendationRepo.upsert(rec)
            log.info("Recommended pattern model=${data.model} fp=${fp.structureHash} count=$c age=${age.seconds}s score=${rec.score}")
        }
    }

    private fun score(count: Long, age: Duration): Double {
        // Simple heuristic: favor frequent and persistent
        val ageMin = age.toMinutes().coerceAtLeast(1)
        return count.toDouble() * Math.log10(1.0 + ageMin)
    }
}