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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryL1CacheTest {

    private val cache = InMemoryL1Cache<CacheKey, TestEntry>()

    private data class TestKey(private val key: String) : CacheKey {
        override fun lookupKey(): String = key
        override fun hasMinimumLoadingCriteria(): Boolean = true
    }

    private data class TestEntry(
        override val cachedAt: Instant = Instant.now(),
        override val expiresAt: Instant = Instant.now()
    ) : CacheEntry<TestEntry> {
        override fun withExpiresAt(expiresAt: Instant): TestEntry = copy(expiresAt = expiresAt)
    }

    @Test
    fun `put stores and get retrieves entry before expiry`() = runTest {
        val key = TestKey("k1")
        val entry = TestEntry()

        cache.put(key, entry, Duration.ofSeconds(5))

        val retrieved = cache.get(key)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.expiresAt).isAfter(Instant.now())
    }

    @Test
    fun `get returns null and invalidates expired entry`() = runTest {
        val key = TestKey("k2")
        val entry = TestEntry()

        // Insert with immediate expiry
        cache.put(key, entry, Duration.ZERO)

        // Simulate time after expiry
        val result = cache.get(key)

        assertThat(result).isNull()
        assertThat(cache.get(key)).isNull() // ensures invalidated
    }

    @Test
    fun `put with ttl null results in immediate expiry`() = runTest {
        val key = TestKey("k3")
        val entry = TestEntry()

        cache.put(key, entry, null)

        val retrieved = cache.get(key)

        assertThat(retrieved).isNull() // expired immediately
    }

    @Test
    fun `invalidate removes entry`() = runTest {
        val key = TestKey("k4")
        val entry = TestEntry()

        cache.put(key, entry, Duration.ofSeconds(5))
        assertThat(cache.get(key)).isNotNull

        cache.invalidate(key)

        assertThat(cache.get(key)).isNull()
    }

    @Test
    fun `get returns entry if not expired`() = runTest {
        val key = TestKey("k5")
        val entry = TestEntry()

        cache.put(key, entry, Duration.ofHours(1))

        val retrieved = cache.get(key)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.expiresAt).isAfter(Instant.now())
    }

}
