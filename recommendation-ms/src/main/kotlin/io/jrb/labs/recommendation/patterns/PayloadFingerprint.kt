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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.MessageDigest

data class PayloadFingerprint(
    val model: String,
    val id: String,
    val structureHash: String
) {
    val key: String get() = "$model#$id#$structureHash"
}

object Fingerprints {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    /**
     * Hash only the structure (keys + value "shapes") of the properties map.
     * Numbers become "N", strings "S", booleans "B", objects "O", arrays "A", null "Z".
     * Keys are sorted so ordering doesn’t change the hash.
     */
    fun structureFingerprint(model: String, id: String, properties: Map<String, Any?>): PayloadFingerprint {
        val normalized = normalize(properties)
        val canonical = mapper.writeValueAsString(normalized) // compact, deterministic
        val sha = sha256(canonical)
        return PayloadFingerprint(model, id, sha)
    }

    private fun normalize(any: Any?): Any? = when (any) {
        null -> mapOf("_" to "Z")
        is Number -> mapOf("_" to "N")
        is String -> mapOf("_" to "S")
        is Boolean -> mapOf("_" to "B")
        is Map<*, *> -> any.entries
            .sortedBy { it.key.toString() }
            .associate { it.key.toString() to normalize(it.value) }
        is Iterable<*> -> listOf("_A", any.firstOrNull()?.let { normalize(it) }) // sample shape
        else -> mapOf("_" to "O")
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

}