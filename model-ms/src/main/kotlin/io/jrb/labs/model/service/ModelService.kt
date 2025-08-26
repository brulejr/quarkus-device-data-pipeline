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
package io.jrb.labs.model.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.eventbus.SystemEventBus
import io.jrb.labs.common.service.ControllableService
import io.jrb.labs.common.service.crud.CrudOutcome
import io.jrb.labs.common.service.crud.CrudService
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.model.model.ModelEntity
import io.jrb.labs.model.repository.ModelRepository
import io.jrb.labs.model.resource.ModelResource
import io.jrb.labs.model.resource.SensorsUpdateRequest
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Startup
@ApplicationScoped
class ModelService(
    override var systemEventBus: SystemEventBus,
    private val modelRepository: ModelRepository,
    private val objectMapper: ObjectMapper
) : ControllableService() {

    private val crudService = CrudService(
        "model",
        repository = modelRepository,
        toResource = { t -> t.toModelResource(objectMapper) },
    )

    @PostConstruct
    override fun startup() {
        super.startup()
    }

    @PreDestroy
    override fun shutdown() {
        super.startup()
    }

    fun findModelResource(modelName: String): CrudOutcome<ModelResource> {
        return runCatching {
            modelRepository.findByModel(modelName).let { foundModel ->
                CrudOutcome.Success(foundModel!!.toModelResource(objectMapper))
            }
        }.getOrElse {
            if (it is NoSuchElementException) {
                CrudOutcome.NotFound(modelName)
            } else {
                CrudOutcome.Error("Failed to find model using name $modelName: ${it.message}", it)
            }
        }
    }

    fun processRawMessage(rtl433Message: Rtl433Message): ModelResource? {
        val modelName = rtl433Message.payload.model
        val jsonString = objectMapper.writeValueAsString(rtl433Message)
        val fingerprint = fingerprint(jsonString)

        val foundModel = modelRepository.findByModel(modelName)
        return if (fingerprint != foundModel?.fingerprint) {
            if (foundModel != null) {
                val model = foundModel.copy(
                    jsonStructure = jsonString,
                    fingerprint = fingerprint
                ).withUpdateInfo()
                modelRepository.update(model)
            } else {
                val model = ModelEntity(
                    source = rtl433Message.source.toString(),
                    model = modelName,
                    jsonStructure = jsonString,
                    fingerprint = fingerprint
                ).withCreateInfo()
                modelRepository.persist(model)
            }
            return modelRepository.findByModel(modelName)?.toModelResource(objectMapper)
        } else null
    }

    suspend fun retrieveModelResources(): CrudOutcome<List<ModelResource>> {
        return crudService.getAll()
    }

    suspend fun updateSensors(modelName: String, request: SensorsUpdateRequest): CrudOutcome<ModelResource> {
        return crudService.updateByField("model", modelName) { existingModel ->
            existingModel.copy(
                category = request.category,
                sensors = request.sensors?.map { it.toSensorMapping() }
            )
        }
    }

    private fun fingerprint(input: String, algorithm: String = "SHA-256"): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

}