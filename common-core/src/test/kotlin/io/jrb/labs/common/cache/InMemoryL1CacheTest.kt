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
package io.jrb.labs.common.cache

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryL1CacheTest {

    private lateinit var cache: InMemoryL1Cache<CacheKey, TestEntry>
    private lateinit var mockKey: TestKey
    private lateinit var mockEntry: TestEntry

    @BeforeEach
    fun setUp() {
        cache = InMemoryL1Cache()
        mockKey = mockk()
        mockEntry = mockk()
    }

    companion object {
        @JvmStatic
        fun cacheScenarios(): Stream<Arguments> = Stream.of(
            Arguments.of("cache miss", false, false, null),   // no entry
            Arguments.of("cache hit", true, false, "value"),  // valid entry
            Arguments.of("cache expired", true, true, null)   // expired entry
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cacheScenarios")
    fun `test cache get scenarios`(
        scenario: String,
        present: Boolean,
        expired: Boolean,
        expectedResult: Any?
    ) = runTest {
        // given
        every { mockKey.lookupKey() } returns "testKey"

        if (present) {
            val expiresAt = if (expired) Instant.now().minusSeconds(10) else Instant.now().plusSeconds(10)
            every { mockEntry.expiresAt } returns expiresAt
            every { mockEntry.cachedAt } returns Instant.now()
            every { mockEntry.withExpiresAt(any()) } returns mockEntry
            cache.put(mockKey, mockEntry, Duration.ofSeconds(60))
        }

        // when
        val result = cache.get(mockKey)

        // then
        if (expectedResult == null) {
            assertThat(result).isNull()
        } else {
            assertThat(result).isNotNull
        }
    }

}
