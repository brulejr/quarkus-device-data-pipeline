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
package io.jrb.labs.ingester.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.messages.RawMessage
import io.jrb.labs.messages.RawMessageSource
import io.jrb.labs.messages.datatypes.Rtl433Data
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class Rtl433Ingester(private val objectMapper: ObjectMapper) {

    private val log by LoggerDelegate()

    @Incoming("rtl433-in")
    @Outgoing("raw-message")
    fun process(payload: String): RawMessage {
        log.info("Raw payload: $payload")
        val rtl433Data = objectMapper.readValue(payload, Rtl433Data::class.java)
        log.info("RTL433 payload: $rtl433Data")
        return RawMessage(source = RawMessageSource.RTL433, payload = payload)
    }

}