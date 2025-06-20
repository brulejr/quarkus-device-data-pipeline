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
package io.jrb.labs.common.service

import io.jrb.labs.common.eventbus.EventBus
import io.jrb.labs.common.eventbus.SystemEvent
import io.jrb.labs.common.eventbus.SystemEventBus
import java.util.concurrent.atomic.AtomicBoolean

abstract class ControllableService {

    abstract val serviceName: String
    abstract var systemEventBus: SystemEventBus

    private val _running = AtomicBoolean(false)
    private lateinit var _subscription: EventBus.Subscription

    open fun startup() {
        _subscription = systemEventBus.subscribe<SystemEvent> {
            if (it.serviceName == serviceName) {
                when (it) {
                    is SystemEvent.StartService -> start()
                    is SystemEvent.StopService -> stop()
                    else -> Unit
                }
            }
        }
        start()
    }

    fun start() {
        if (_running.compareAndSet(false, true)) {
            systemMessage("service.starting")
            onStart()
            systemMessage("service.started")
        }
    }

    fun stop() {
        if (_running.compareAndSet(true, false)) {
            systemMessage("service.stopping")
            onStop()
            systemMessage("service.stopped")
        }
    }

    fun isRunning(): Boolean = _running.get()

    open fun shutdown() {
        stop()
        _subscription.cancel()
    }

    protected open fun onStart() {}
    protected open fun onStop() {}

    private fun systemMessage(code: String) {
        systemEventBus.send(SystemEvent.SystemMessage(serviceName, code))
    }

}