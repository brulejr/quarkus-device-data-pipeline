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
package io.jrb.labs.model.model

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.model.Entity
import io.jrb.labs.model.resource.ModelResource
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.Instant

data class ModelEntity @BsonCreator constructor(
    @BsonId override val id: ObjectId? = null,
    @BsonProperty("guid") override val guid: String? = null,
    @BsonProperty("ownerGuid") override val ownerGuid: String? = null,
    @BsonProperty("source") val source: String,
    @BsonProperty("model") val model: String,
    @BsonProperty("jsonStructure") val jsonStructure: String,
    @BsonProperty("fingerprint") val fingerprint: String,
    @BsonProperty("category") val category: String? = null,
    @BsonProperty("sensors") val sensors: List<SensorMapping>? = null,
    @BsonProperty("createdOn") override val createdOn: Instant? = null,
    @BsonProperty("createdBy") override val createdBy: String? = null,
    @BsonProperty("modifiedOn") override val modifiedOn: Instant? = null,
    @BsonProperty("modifiedBy") override val modifiedBy: String? = null,
    @BsonProperty("version")  override val version: Long? = null
) : Entity<ModelEntity>, PanacheMongoEntityBase() {

    override fun withCreateInfo(guid: String?, userGuid: String?): ModelEntity {
        return copy(
            guid = guid,
            ownerGuid = userGuid,
            createdBy = userGuid,
            createdOn = Instant.now(),
            version = 1
        )
    }

    override fun withUpdateInfo(userGuid: String?): ModelEntity {
        return copy(
            modifiedBy = userGuid,
            modifiedOn = Instant.now(),
            version = version?.plus(1)
        )
    }

    fun toModelResource(objectMapper: ObjectMapper): ModelResource {
        return ModelResource(
            source = this.source,
            model = this.model,
            jsonStructure = this.jsonStructure.let { objectMapper.readTree(it) },
            fingerprint = this.fingerprint,
            category = category,
            sensors = this.sensors?.map { it.toSensorMappingResource() },
            createdOn = this.createdOn,
            modifiedOn = this.modifiedOn,
            version = this.version
        )
    }

}
