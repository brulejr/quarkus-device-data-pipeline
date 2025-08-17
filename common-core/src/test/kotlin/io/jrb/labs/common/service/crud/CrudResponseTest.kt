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
package io.jrb.labs.common.service.crud

import io.mockk.*
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

class CrudResponseTest {

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `crudResponse should handle Success outcome with default success function`() {
        runBlocking {
            val testData = "test data"
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success(testData) }

            val response = CrudResponse.crudResponse(actionFn)

            assertThat(response.status).isEqualTo(200)
            assertThat(response.entity).isEqualTo(testData)
        }
    }

    @Test
    fun `crudResponse should handle Success outcome with custom success function`() {
        runBlocking {
            val testData = TestEntity("123", "Test Name")
            val actionFn: suspend () -> CrudOutcome<TestEntity> = { CrudOutcome.Success(testData) }
            val mockResponse = mockk<Response>()
            val customSuccessFn: (CrudOutcome.Success<TestEntity>) -> Response = mockk()

            every { customSuccessFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                successFn = customSuccessFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customSuccessFn(CrudOutcome.Success(testData)) }
        }
    }

    @Test
    fun `crudResponse should handle NotFound outcome with default function throwing exception`() {
        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.NotFound("123") }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(InternalServerErrorException::class.java)
            .hasMessage("Unsupported operation")
    }

    @Test
    fun `crudResponse should handle NotFound outcome with custom function`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.NotFound("123") }
            val mockResponse = mockk<Response>()
            val customNotFoundFn: (CrudOutcome.NotFound) -> Response = mockk()

            every { customNotFoundFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                notFoundFn = customNotFoundFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customNotFoundFn(CrudOutcome.NotFound("123")) }
        }
    }

    @Test
    fun `crudResponse should handle Conflict outcome with default function throwing exception`() {
        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Conflict("Entity already exists") }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(InternalServerErrorException::class.java)
            .hasMessage("Unsupported operation")
    }

    @Test
    fun `crudResponse should handle Conflict outcome with custom function`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Conflict("Duplicate key") }
            val mockResponse = mockk<Response>()
            val customConflictFn: (CrudOutcome.Conflict) -> Response = mockk()

            every { customConflictFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                conflictFn = customConflictFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customConflictFn(CrudOutcome.Conflict("Duplicate key")) }
        }
    }

    @Test
    fun `crudResponse should handle Invalid outcome with default function throwing exception`() {
        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Invalid("Missing required field") }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(InternalServerErrorException::class.java)
            .hasMessage("Unsupported operation")
    }

    @Test
    fun `crudResponse should handle Invalid outcome with custom function`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Invalid("Bad format") }
            val mockResponse = mockk<Response>()
            val customInvalidFn: (CrudOutcome.Invalid) -> Response = mockk()

            every { customInvalidFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                invalidFn = customInvalidFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customInvalidFn(CrudOutcome.Invalid("Bad format")) }
        }
    }

    @Test
    fun `crudResponse should handle Error outcome with default function throwing exception`() {
        val cause = RuntimeException("Database connection failed")

        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Error("Service error", cause) }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(InternalServerErrorException::class.java)
            .hasMessage("Service error")
            .hasCause(cause)
    }

    @Test
    fun `crudResponse should handle Error outcome without cause`() {
        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Error("Service error") }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(InternalServerErrorException::class.java)
            .hasMessage("Service error")
            .hasNoCause()
    }

    @Test
    fun `crudResponse should handle Error outcome with custom function`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Error("Custom error") }
            val mockResponse = mockk<Response>()
            val customErrorFn: (CrudOutcome.Error) -> Response = mockk()

            every { customErrorFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                errorFn = customErrorFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customErrorFn(CrudOutcome.Error("Custom error")) }
        }
    }

    @Test
    fun `crudResponse should work with all custom handlers`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success("test") }
            val mockResponse = mockk<Response>()
            val customSuccessFn: (CrudOutcome.Success<String>) -> Response = mockk()
            val customNotFoundFn: (CrudOutcome.NotFound) -> Response = mockk()
            val customConflictFn: (CrudOutcome.Conflict) -> Response = mockk()
            val customInvalidFn: (CrudOutcome.Invalid) -> Response = mockk()
            val customErrorFn: (CrudOutcome.Error) -> Response = mockk()

            every { customSuccessFn(any()) } returns mockResponse

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                successFn = customSuccessFn,
                notFoundFn = customNotFoundFn,
                conflictFn = customConflictFn,
                invalidFn = customInvalidFn,
                errorFn = customErrorFn
            )

            assertThat(response).isEqualTo(mockResponse)
            verify { customSuccessFn(CrudOutcome.Success("test")) }
            verify(exactly = 0) { customNotFoundFn(any()) }
            verify(exactly = 0) { customConflictFn(any()) }
            verify(exactly = 0) { customInvalidFn(any()) }
            verify(exactly = 0) { customErrorFn(any()) }
        }
    }

    @Test
    fun `crudResponse should handle exceptions thrown by actionFn`() {
        val testException = RuntimeException("Action function failed")

        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { throw testException }
                CrudResponse.crudResponse(actionFn)
            }
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Action function failed")
    }

    @Test
    fun `crudResponse should handle exceptions thrown by custom handler functions`() {
        val testException = RuntimeException("Handler function failed")

        assertThatThrownBy {
            runBlocking {
                val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success("test") }
                val customSuccessFn: (CrudOutcome.Success<String>) -> Response = { throw testException }

                CrudResponse.crudResponse(
                    actionFn = actionFn,
                    successFn = customSuccessFn
                )
            }
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Handler function failed")
    }

    @Test
    fun `crudResponse should work with complex data types`() {
        runBlocking {
            val complexData = mapOf("users" to listOf(TestEntity("1", "User1"), TestEntity("2", "User2")))
            val actionFn: suspend () -> CrudOutcome<Map<String, List<TestEntity>>> = { CrudOutcome.Success(complexData) }

            val response = CrudResponse.crudResponse(actionFn)

            assertThat(response.status).isEqualTo(200)
            assertThat(response.entity).isEqualTo(complexData)
        }
    }

    @Test
    fun `crudResponse should work with nullable data types`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String?> = { CrudOutcome.Success(null) }

            val response = CrudResponse.crudResponse(actionFn)

            assertThat(response.status).isEqualTo(200)
            assertThat(response.entity).isNull()
        }
    }

    @Test
    fun `crudResponse should be callable multiple times with same parameters`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success("consistent result") }

            val response1 = CrudResponse.crudResponse(actionFn)
            val response2 = CrudResponse.crudResponse(actionFn)

            assertThat(response1.status).isEqualTo(200)
            assertThat(response2.status).isEqualTo(200)
            assertThat(response1.entity).isEqualTo("consistent result")
            assertThat(response2.entity).isEqualTo("consistent result")
        }
    }

    @Test
    fun `crudResponse should handle suspending actionFn correctly`() {
        runBlocking {
            val actionFn: suspend () -> CrudOutcome<String> = {
                // Simulate some async work
                kotlinx.coroutines.delay(1)
                CrudOutcome.Success("async result")
            }

            val response = CrudResponse.crudResponse(actionFn)

            assertThat(response.status).isEqualTo(200)
            assertThat(response.entity).isEqualTo("async result")
        }
    }

    @Test
    fun `default success function should create proper JAX-RS response`() {
        runBlocking {
            val testData = "test response data"
            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success(testData) }

            val response = CrudResponse.crudResponse(actionFn)

            assertThat(response.status).isEqualTo(Response.Status.OK.statusCode)
            assertThat(response.entity).isEqualTo(testData)
            assertThat(response.statusInfo).isEqualTo(Response.Status.OK)
        }
    }

    @Test
    fun `should verify handler function signatures are correct`() {
        runBlocking {
            // This test ensures that the function signatures match what's expected
            val successFn: (CrudOutcome.Success<String>) -> Response = { Response.ok(it.data).build() }
            val notFoundFn: (CrudOutcome.NotFound) -> Response = { Response.status(404).build() }
            val conflictFn: (CrudOutcome.Conflict) -> Response = { Response.status(409).build() }
            val invalidFn: (CrudOutcome.Invalid) -> Response = { Response.status(400).build() }
            val errorFn: (CrudOutcome.Error) -> Response = { Response.status(500).build() }

            val actionFn: suspend () -> CrudOutcome<String> = { CrudOutcome.Success("test") }

            val response = CrudResponse.crudResponse(
                actionFn = actionFn,
                successFn = successFn,
                notFoundFn = notFoundFn,
                conflictFn = conflictFn,
                invalidFn = invalidFn,
                errorFn = errorFn
            )

            assertThat(response.status).isEqualTo(200)
        }
    }

    // Helper data class for testing
    data class TestEntity(val id: String, val name: String)

}