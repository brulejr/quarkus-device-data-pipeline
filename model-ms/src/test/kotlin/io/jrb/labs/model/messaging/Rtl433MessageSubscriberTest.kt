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
package io.jrb.labs.model.messaging

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.jrb.labs.messages.RawMessageSource
import io.jrb.labs.messages.Rtl433Message
import io.jrb.labs.model.service.ModelService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import org.assertj.core.api.Assertions.assertThat
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class Rtl433MessageSubscriberTest {

    private lateinit var modelService: ModelService
    private lateinit var log: Logger
    private lateinit var subscriber: Rtl433MessageSubscriber

    @BeforeEach
    fun setup() {
        // Mutate existing Vert.x mappers to register the Kotlin module
        DatabindCodec.mapper().registerKotlinModule()
        DatabindCodec.prettyMapper().registerKotlinModule()

        // Mocks
        modelService = mockk(relaxed = true)
        log = mockk(relaxed = true)

        subscriber = Rtl433MessageSubscriber(modelService, log)
    }

    @Test
    fun `process should map JsonObject to Rtl433Message and call modelService`() {
        // Given
        val timestamp = Instant.parse("2025-08-16T14:00:00Z")
        val id = UUID.randomUUID()
        val json = JsonObject(
            """
            {
              "source": "RTL433",
              "id": "$id",
              "timestamp": "$timestamp",
              "payload": {
                "model": "TestModel",
                "id": "sensor-123",
                "time": "$timestamp",
                "name": "Test Sensor",
                "type": "temperature",
                "area": "living-room",
                "foo": "bar"
              }
            }
            """.trimIndent()
        )

        // When
        subscriber.process(json)

        // Then
        val slot = slot<Rtl433Message>()
        verify { modelService.processRawMessage(capture(slot)) }

        val capturedMessage = slot.captured
        println("*** capturedMessage: $capturedMessage")
        assertThat(capturedMessage.source).isEqualTo(RawMessageSource.RTL433)
        assertThat(capturedMessage.id).isEqualTo(id)
        assertThat(capturedMessage.timestamp).isEqualTo(timestamp)

        val payload = capturedMessage.payload
        println("*** payload: $payload")
        assertThat(payload.model).isEqualTo("TestModel")
        assertThat(payload.id).isEqualTo("sensor-123")
        assertThat(payload.time).isEqualTo(timestamp)
        assertThat(payload.get("name")).isEqualTo("Test Sensor")
        assertThat(payload.get("type")).isEqualTo("temperature")
        assertThat(payload.get("area")).isEqualTo("living-room")
        assertThat(payload.getProperties()).containsEntry("foo", "bar")

        verify { log.info(match { it.toString().contains("rtl433Message") }) }
    }

    @Test
    fun `process should log the rtl433Message`() {
        val json = JsonObject(
            """
            {
              "source": "RTL433",
              "id": "${UUID.randomUUID()}",
              "timestamp": "2025-08-16T14:00:00Z",
              "payload": {
                "model": "SimpleModel",
                "id": "sensor-999",
                "time": "2025-08-16T14:00:00Z"
              }
            }
            """.trimIndent()
        )

        subscriber.process(json)

        verify { log.info(match { it.toString().contains("rtl433Message") }) }
    }

}
