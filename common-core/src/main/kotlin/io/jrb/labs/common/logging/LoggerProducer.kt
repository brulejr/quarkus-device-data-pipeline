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
package io.jrb.labs.common.logging

import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Produces
import jakarta.enterprise.inject.spi.InjectionPoint
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Dependent
class LoggerProducer {

    @Produces
    fun produceLogger(injectionPoint: InjectionPoint): Logger {
        // Try to get @LoggerName from the annotated parameter or field
        val annotation = injectionPoint.annotated
            .getAnnotation(LoggerName::class.java)

        // Fallback to the declaring class
        val className = annotation?.value?.takeIf { it.isNotEmpty() }
            ?: injectionPoint.member.declaringClass.name

        return LoggerFactory.getLogger(className)
    }

}
