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

import io.jrb.labs.recommendation.model.KnownPatternEntity
import io.jrb.labs.recommendation.repository.KnownPatternRepo
import io.jrb.labs.recommendation.repository.RecommendationRepo
import io.jrb.labs.recommendation.resource.PromoteRequest
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api" +
        "/promotions")
class PromoteResource(
    private val knownRepo: KnownPatternRepo,
    private val recRepo: RecommendationRepo
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<KnownPatternEntity> =
        knownRepo.findAll().list()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    fun promote(req: PromoteRequest): Response {
        val entity = KnownPatternEntity(
            key = "${req.model}#${req.id}#${req.fingerprint}",
            deviceModel = req.model,
            deviceId = req.id,
            fingerprint = req.fingerprint,
            name = req.name,
            type = req.type,
            area = req.area
        )
        knownRepo.upsert(entity)
        // Remove/suppress any outstanding recommendations for this pattern
        recRepo.deleteByKey(req.model, req.id, req.fingerprint)
        return Response.ok(entity).build()
    }

    @DELETE
    @Path("/{model}/{id}/{fingerprint}")
    fun delete(
        @PathParam("model") model: String,
        @PathParam("id") id: String,
        @PathParam("fingerprint") fingerprint: String
    ): Response {
        knownRepo.deleteByKey(model, id, fingerprint)
        return Response.noContent().build()
    }

}