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
package io.jrb.labs.model.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ModelResource(

    @field:JsonView(ModelViews.List::class)
    val source: String,

    @field:JsonView(ModelViews.List::class)
    val model: String,

    @field:JsonView(ModelViews.List::class)
    val category: String?,

    @field:JsonView(ModelViews.Details::class)
    val jsonStructure: JsonNode,

    @field:JsonView(ModelViews.Details::class)
    val fingerprint: String,

    @field:JsonView(ModelViews.Details::class)
    val sensors: List<SensorMappingResource>? = null,

    @field:JsonView(ModelViews.Details::class)
    val createdOn: Instant? = null,

    @field:JsonView(ModelViews.Details::class)
    val modifiedOn: Instant? = null,

    @field:JsonView(ModelViews.Details::class)
    val version: Long? = null

)