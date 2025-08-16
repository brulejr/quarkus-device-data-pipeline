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
import io.jrb.labs.datatypes.Rtl433Data
import io.jrb.labs.messages.RawMessageSource
import io.jrb.labs.messages.Rtl433Message
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class Rtl433IngesterTest {

    private val objectMapper: ObjectMapper = mockk()
    private val mockLogger: Logger = mockk(relaxed = true)
    private lateinit var ingester: Rtl433Ingester

    @BeforeEach
    fun setUp() {
        // Inject mock logger directly
        ingester = Rtl433Ingester(objectMapper, mockLogger)
    }

    @Test
    fun `process should convert JSON to Rtl433Message and log`() {
        val fakeData = Rtl433Data(
            model = "TestModel",
            id = "123",
            time = Instant.parse("2025-08-14T12:34:56Z"),
            properties = mapOf("temp" to 25)
        )
        every { objectMapper.readValue(any<String>(), Rtl433Data::class.java) } returns fakeData

        val jsonPayload = """{"model":"TestModel","id":"123","time":"2025-08-14T12:34:56Z","temp":25}"""

        val result: Rtl433Message = ingester.process(jsonPayload)

        assertThat(result.source).isEqualTo(RawMessageSource.RTL433)
        assertThat(result.payload).isEqualTo(fakeData)

        verify { mockLogger.info("RTL433 payload: ${fakeData}") }
    }

    @Test
    fun `process should throw exception and not log payload if JSON is invalid`() {
        every { objectMapper.readValue(any<String>(), Rtl433Data::class.java) } throws RuntimeException("Invalid JSON")
        val badPayload = """{"model":"MissingFields"}"""

        assertThatThrownBy { ingester.process(badPayload) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Invalid JSON")

        // Explicitly declare matcher types to fix compile error
        verify(exactly = 0) { mockLogger.info(any<String>()) }
    }

}
