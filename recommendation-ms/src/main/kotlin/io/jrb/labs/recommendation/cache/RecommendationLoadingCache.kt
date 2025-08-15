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
package io.jrb.labs.recommendation.cache

import io.jrb.labs.common.cache.L1Cache
import io.jrb.labs.common.cache.LoadingL1L2Cache
import java.time.Duration

class RecommendationLoadingCache(
    l1Cache: L1Cache<RecommendationCacheKey, RecommendationCacheEntry>,
    store: RecommendationCacheStore,
    ttlL1Cache: Duration = Duration.ofMinutes(3),
    ttlL2Cache: Duration = Duration.ofDays(1),
    loadingFn: suspend (RecommendationCacheKey) -> RecommendationCacheEntry? = { null }
) : LoadingL1L2Cache<RecommendationCacheKey, RecommendationCacheEntry>(
    l1Cache = l1Cache,
    store = store,
    ttlL1Cache = ttlL1Cache,
    ttlL2Cache = ttlL2Cache,
    loadingFn = loadingFn
)