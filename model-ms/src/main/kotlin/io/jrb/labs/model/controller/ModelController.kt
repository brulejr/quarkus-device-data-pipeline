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
package io.jrb.labs.model.controller

import com.fasterxml.jackson.annotation.JsonView
import io.jrb.labs.common.service.crud.CrudResponse.Companion.crudResponse
import io.jrb.labs.model.resource.ModelViews
import io.jrb.labs.model.resource.SensorsUpdateRequest
import io.jrb.labs.model.service.ModelService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.RestPath

@ApplicationScoped
@Path("/api/models")
class ModelController(private val modelService: ModelService) {

    @GET
    @Path("/{modelName}")
    @JsonView(ModelViews.Details::class)
    suspend fun getModel(@RestPath modelName: String): Response {
        return crudResponse(
            actionFn = { modelService.findModelResource(modelName) },
            notFoundFn = { throw NotFoundException("Unable to find model by name: $modelName") }
        )
    }

    @GET
    @JsonView(ModelViews.List::class)
    suspend fun retrieve(): Response {
        return crudResponse(actionFn = { modelService.retrieveModelResources() })
    }

    @PUT
    @Path("/{modelName}/sensors")
    @JsonView(ModelViews.Details::class)
    suspend fun updateSensors(@RestPath modelName: String, request: SensorsUpdateRequest): Response {
        return crudResponse(
            actionFn = { modelService.updateSensors(modelName, request) },
            notFoundFn = { throw NotFoundException("Unable to find model by name: $modelName") }
        )
    }

}