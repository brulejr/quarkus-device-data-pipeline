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
package io.jrb.labs.recommendation.model

import io.jrb.labs.recommendation.resource.RecommendationResource
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.Instant

data class RecommendationEntity @BsonCreator constructor(
    @BsonId val id: ObjectId? = null,
    @BsonProperty("key") val key: String,
    @BsonProperty("deviceModel") val deviceModel: String,
    @BsonProperty("deviceId") val deviceId: String,
    @BsonProperty("fingerprint") val fingerprint: String,
    @BsonProperty("examples") val examples: Int,
    @BsonProperty("score") val score: Double,
    @BsonProperty("lastEmittedAt") val lastEmittedAt: Instant
) : PanacheMongoEntityBase() {

    fun toRecommendationResource(): RecommendationResource {
        return RecommendationResource(
            model = deviceModel,
            id = deviceId,
            fingerprint = fingerprint,
            examples = examples,
            score = score,
            lastEmittedAt = lastEmittedAt
        )
    }

    companion object {
        fun from(
            deviceModel: String,
            deviceId: String,
            fingerprint: String,
            examples: Int,
            score: Double,
            lastEmittedAt: Instant
        ) =
            RecommendationEntity(
                key = "$deviceModel#$deviceId#$fingerprint",
                deviceModel = deviceModel,
                deviceId = deviceId,
                fingerprint = fingerprint,
                examples = examples,
                score = score,
                lastEmittedAt = lastEmittedAt
            )
    }

}