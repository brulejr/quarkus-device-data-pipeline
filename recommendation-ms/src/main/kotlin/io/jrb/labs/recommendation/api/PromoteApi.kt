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
package io.jrb.labs.recommendation.api

import io.jrb.labs.recommendation.resource.KnownPatternResource
import io.jrb.labs.recommendation.resource.PromoteRequest
import io.jrb.labs.recommendation.service.RecommendationService
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/promotions")
class PromoteApi(
    private val resourceService: RecommendationService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<KnownPatternResource> = resourceService.knownPatterns()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun promote(req: PromoteRequest): Response {
        resourceService.promoteRecommendation(req)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/{model}/{id}/{fingerprint}")
    fun delete(
        @PathParam("model") model: String,
        @PathParam("id") id: String,
        @PathParam("fingerprint") fingerprint: String
    ): Response {
        resourceService.deletePromotion(model, id, fingerprint)
        return Response.noContent().build()
    }

}