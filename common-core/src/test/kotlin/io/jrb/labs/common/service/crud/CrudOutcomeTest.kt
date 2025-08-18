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

import io.jrb.labs.common.test.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrudOutcomeTest : TestUtils {

    @Test
    fun `Success should contain provided data`() {
        val testData = randomString()
        val success = CrudOutcome.Success(testData)

        assertThat(success.data).isEqualTo(testData)
        assertThat(success).isInstanceOf(CrudOutcome.Success::class.java)
    }

    @Test
    fun `Success should work with different data types`() {
        val stringData = randomString()
        val intData = randomInt()
        val listData = randomList<Int>(3, this::randomInt)
        val objectData = TestEntity(randomString(), randomString())

        val stringSuccess = CrudOutcome.Success(stringData)
        val intSuccess = CrudOutcome.Success(intData)
        val listSuccess = CrudOutcome.Success(listData)
        val customObjectSuccess = CrudOutcome.Success(objectData)

        assertThat(stringSuccess.data).isEqualTo(stringData)
        assertThat(intSuccess.data).isEqualTo(intData)
        assertThat(listSuccess.data).containsAll(listData)
        assertThat(customObjectSuccess.data).isEqualTo(objectData)
    }

    @Test
    fun `Success should work with mocked objects`() {
        val mockEntity = mockk<TestEntity>()
        every { mockEntity.id } returns "mock-id"
        every { mockEntity.name } returns "mock-name"

        val success = CrudOutcome.Success(mockEntity)

        assertThat(success.data).isEqualTo(mockEntity)
        assertThat(success.data.id).isEqualTo("mock-id")
        assertThat(success.data.name).isEqualTo("mock-name")
    }

    @Test
    fun `Success equality should work correctly`() {
        val success1 = CrudOutcome.Success(randomString())
        val success2 = CrudOutcome.Success(success1.data)
        val success3 = CrudOutcome.Success("different")

        assertThat(success1).isEqualTo(success2)
        assertThat(success1).isNotEqualTo(success3)
        assertThat(success1.hashCode()).isEqualTo(success2.hashCode())
    }

    @Test
    fun `NotFound should contain provided id`() {
        val testId = randomString()
        val notFound = CrudOutcome.NotFound(testId)

        assertThat(notFound.id).isEqualTo(testId)
        assertThat(notFound).isInstanceOf(CrudOutcome.NotFound::class.java)
    }

    @Test
    fun `NotFound equality should work correctly`() {
        val notFound1 = CrudOutcome.NotFound(randomString())
        val notFound2 = CrudOutcome.NotFound(notFound1.id)
        val notFound3 = CrudOutcome.NotFound(randomString())

        assertThat(notFound1).isEqualTo(notFound2)
        assertThat(notFound1).isNotEqualTo(notFound3)
        assertThat(notFound1.hashCode()).isEqualTo(notFound2.hashCode())
    }

    @Test
    fun `Conflict should contain provided reason`() {
        val testReason = randomString()
        val conflict = CrudOutcome.Conflict(testReason)

        assertThat(conflict.reason).isEqualTo(testReason)
        assertThat(conflict).isInstanceOf(CrudOutcome.Conflict::class.java)
    }

    @Test
    fun `Conflict equality should work correctly`() {
        val conflict1 = CrudOutcome.Conflict(randomString())
        val conflict2 = CrudOutcome.Conflict(conflict1.reason)
        val conflict3 = CrudOutcome.Conflict(randomString())

        assertThat(conflict1).isEqualTo(conflict2)
        assertThat(conflict1).isNotEqualTo(conflict3)
        assertThat(conflict1.hashCode()).isEqualTo(conflict2.hashCode())
    }

    @Test
    fun `Invalid should contain provided reason`() {
        val testReason = randomString()
        val invalid = CrudOutcome.Invalid(testReason)

        assertThat(invalid.reason).isEqualTo(testReason)
        assertThat(invalid).isInstanceOf(CrudOutcome.Invalid::class.java)
    }

    @Test
    fun `Invalid equality should work correctly`() {
        val invalid1 = CrudOutcome.Invalid(randomString())
        val invalid2 = CrudOutcome.Invalid(invalid1.reason)
        val invalid3 = CrudOutcome.Invalid(randomString())

        assertThat(invalid1).isEqualTo(invalid2)
        assertThat(invalid1).isNotEqualTo(invalid3)
        assertThat(invalid1.hashCode()).isEqualTo(invalid2.hashCode())
    }

    @Test
    fun `Error should contain message and optional cause`() {
        val testMessage = randomString()
        val testCause = RuntimeException(randomString())

        val errorWithCause = CrudOutcome.Error(testMessage, testCause)
        val errorWithoutCause = CrudOutcome.Error(testMessage)

        assertThat(errorWithCause.message).isEqualTo(testMessage)
        assertThat(errorWithCause.cause).isEqualTo(testCause)
        assertThat(errorWithoutCause.message).isEqualTo(testMessage)
        assertThat(errorWithoutCause.cause).isNull()
    }

    @Test
    fun `Error should work with mocked exceptions`() {
        val mockExceptionMessage = randomString()
        val mockException = mockk<RuntimeException>()
        every { mockException.message } returns mockExceptionMessage

        val serviceErrorMessage = randomString()
        val error = CrudOutcome.Error(serviceErrorMessage, mockException)

        assertThat(error.message).isEqualTo(serviceErrorMessage)
        assertThat(error.cause).isEqualTo(mockException)
        assertThat(error.cause?.message).isEqualTo(mockExceptionMessage)
    }

    @Test
    fun `Error equality should work correctly`() {
        val exception = RuntimeException(randomString())
        val error1 = CrudOutcome.Error(randomString(), exception)
        val error2 = CrudOutcome.Error(error1.message, exception)
        val error3 = CrudOutcome.Error(randomString(), exception)
        val error4 = CrudOutcome.Error(error1.message, RuntimeException(randomString()))
        val error5 = CrudOutcome.Error(error1.message)

        assertThat(error1).isEqualTo(error2)
        assertThat(error1).isNotEqualTo(error3)
        assertThat(error1).isNotEqualTo(error4)
        assertThat(error1).isNotEqualTo(error5)
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode())
    }

    @Test
    fun `Different outcome types should not be equal`() {
        val success = CrudOutcome.Success(randomString())
        val notFound = CrudOutcome.NotFound(randomString())
        val conflict = CrudOutcome.Conflict(randomString())
        val invalid = CrudOutcome.Invalid(randomString())
        val error = CrudOutcome.Error(randomString())

        val outcomes = listOf(success, notFound, conflict, invalid, error)

        // Test that each outcome is not equal to any other type
        for (i in outcomes.indices) {
            for (j in outcomes.indices) {
                if (i != j) {
                    assertThat(outcomes[i]).isNotEqualTo(outcomes[j])
                }
            }
        }
    }

    @Test
    fun `toString should provide meaningful representation`() {
        val successData = randomString()
        val notFoundId = randomString()
        val conflictReason = randomString()
        val invalidReason = randomString()
        val errorMessage = randomString()
        val success = CrudOutcome.Success(successData)
        val notFound = CrudOutcome.NotFound(notFoundId)
        val conflict = CrudOutcome.Conflict(conflictReason)
        val invalid = CrudOutcome.Invalid(invalidReason)
        val error = CrudOutcome.Error(errorMessage, RuntimeException("timeout"))

        assertThat(success.toString()).contains("Success", successData)
        assertThat(notFound.toString()).contains("NotFound", notFoundId)
        assertThat(conflict.toString()).contains("Conflict", conflictReason)
        assertThat(invalid.toString()).contains("Invalid", invalidReason)
        assertThat(error.toString()).contains("Error", errorMessage)
    }

    @Test
    fun `Sealed class hierarchy should work with when expressions`() {
        val outcomes: List<CrudOutcome<String>> = listOf(
            CrudOutcome.Success(randomString()),
            CrudOutcome.NotFound(randomString()),
            CrudOutcome.Conflict(randomString()),
            CrudOutcome.Invalid(randomString()),
            CrudOutcome.Error(randomString())
        )

        outcomes.forEach { outcome ->
            val result = when (outcome) {
                is CrudOutcome.Success -> "success: ${outcome.data}"
                is CrudOutcome.NotFound -> "not found: ${outcome.id}"
                is CrudOutcome.Conflict -> "conflict: ${outcome.reason}"
                is CrudOutcome.Invalid -> "invalid: ${outcome.reason}"
                is CrudOutcome.Error -> "error: ${outcome.message}"
            }

            assertThat(result).isNotBlank()
        }
    }

    @Test
    fun `Success should be contravariant in type parameter`() {
        val stringSuccess: CrudOutcome<String> = CrudOutcome.Success(randomString())
        val anySuccess: CrudOutcome<Any> = stringSuccess

        // This should compile without issues due to covariance of the type parameter
        assertThat(anySuccess).isInstanceOf(CrudOutcome.Success::class.java)
    }

    @Test
    fun `Should handle null data in Success gracefully`() {
        val nullSuccess = CrudOutcome.Success<String?>(null)

        assertThat(nullSuccess.data).isNull()
        assertThat(nullSuccess).isInstanceOf(CrudOutcome.Success::class.java)
    }

    @Test
    fun `Should handle empty strings correctly`() {
        val emptyStringId = CrudOutcome.NotFound("")
        val emptyStringReason = CrudOutcome.Conflict("")
        val emptyStringInvalid = CrudOutcome.Invalid("")
        val emptyStringError = CrudOutcome.Error("")

        assertThat(emptyStringId.id).isEmpty()
        assertThat(emptyStringReason.reason).isEmpty()
        assertThat(emptyStringInvalid.reason).isEmpty()
        assertThat(emptyStringError.message).isEmpty()
    }

    @Test
    fun `Should work with complex generic types`() {
        val complexData = mapOf("key1" to listOf("a", "b"), "key2" to listOf("c", "d"))
        val success = CrudOutcome.Success(complexData)

        assertThat(success.data).hasSize(2)
        assertThat(success.data["key1"]).containsExactly("a", "b")
        assertThat(success.data["key2"]).containsExactly("c", "d")
    }

    // Helper data class for testing
    data class TestEntity(val id: String, val name: String)

}