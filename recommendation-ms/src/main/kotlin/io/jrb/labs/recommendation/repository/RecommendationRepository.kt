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
package io.jrb.labs.recommendation.repository

import io.jrb.labs.recommendation.model.RecommendationEntity
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RecommendationRepository : PanacheMongoRepository<RecommendationEntity> {

    fun deleteByKey(model: String, id: String, fingerprint: String): Long =
        delete("key","$model#$id#$fingerprint")

    fun findByKey(model: String, id: String, fingerprint: String): RecommendationEntity? =
        find("key","$model#$id#$fingerprint").firstResult()

    fun top(limit: Int = 20): List<RecommendationEntity> =
        findAll().stream().sorted { a, b -> b.score.compareTo(a.score) }.limit(limit.toLong()).toList()

    fun upsert(entity: RecommendationEntity) {
        val existing = findByKey(entity.deviceModel, entity.deviceId, entity.fingerprint)
        if (existing == null) {
            persist(entity)
        } else {
            update(existing.copy(
                examples = entity.examples,
                score = maxOf(existing.score, entity.score),
                lastEmittedAt = entity.lastEmittedAt
            ))
        }
    }

}