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
package io.jrb.labs.recommendation.patterns

import io.jrb.labs.datatypes.Rtl433Data
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.AutoEncoder
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.jboss.logging.Logger
import io.quarkus.scheduler.Scheduled
import org.deeplearning4j.nn.conf.layers.OutputLayer
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.abs

@ApplicationScoped
class AutoencoderAnomalyDetector : AnomalyDetector {

    private val log = Logger.getLogger(javaClass)

    // Fixed feature dimension for hashing trick
    private val dim = 128

    // Model + buffer
    @Volatile private lateinit var net: MultiLayerNetwork
    private val buffer = ConcurrentLinkedDeque<DoubleArray>() // rolling store
    private val maxBuffer = 4000
    private val batchSize = 64
    private val trainItersPerTick = 4

    // Scoring
    private val anomalyThreshold = 0.03 // tune on your data (reconstruction MSE)

    @PostConstruct
    fun init() {
        net = MultiLayerNetwork(config(dim))
        net.init()
        net.setListeners(ScoreIterationListener(0)) // no spam; set >0 to log
        log.info("Autoencoder initialized with input dim=$dim")
    }

    override fun anomalyScore(data: Rtl433Data): Double? {
        val v = toVector(data) ?: return null
        // store for training
        if (buffer.size >= maxBuffer) buffer.pollFirst()
        buffer.offerLast(v)

        // compute reconstruction error
        val x = Nd4j.create(v)
        val y = net.output(x, false)
        val err = mse(x, y)
        return err
    }

    // Train periodically from the rolling buffer
    @Scheduled(every = "10s")
    fun trainTick() {
        if (buffer.size < batchSize) return
        repeat(trainItersPerTick) {
            val batch = sampleBatch()
            val ds = toDataSet(batch)
            net.fit(ds)
        }
    }

    private fun config(nIn: Int): MultiLayerConfiguration =
        NeuralNetConfiguration.Builder()
            .seed(42)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(Adam(1e-3))
            .list()
            .layer(
                0, AutoEncoder.Builder()
                    .nIn(nIn).nOut(64)
                    .activation(Activation.RELU)
                    .lossFunction(LossFunctions.LossFunction.MSE)
                    .build()
            )
            .layer(
                1, AutoEncoder.Builder()
                    .nIn(64).nOut(16)    // bottleneck
                    .activation(Activation.RELU)
                    .lossFunction(LossFunctions.LossFunction.MSE)
                    .build()
            )
            .layer(
                2, AutoEncoder.Builder()
                    .nIn(16).nOut(64)
                    .activation(Activation.RELU)
                    .lossFunction(LossFunctions.LossFunction.MSE)
                    .build()
            )
            .layer(3, OutputLayer.Builder(LossFunctions.LossFunction.MSE) // reconstruction loss
                .activation(Activation.IDENTITY)
                .nIn(64).nOut(nIn) // output matches input dimension
                .build()
            )
            .build()

    private fun toVector(data: Rtl433Data): DoubleArray? {
        val props = data.getProperties()
        if (props.isEmpty()) return null
        val v = DoubleArray(dim) { 0.0 }
        fun put(idx: Int, value: Double) { v[idx % dim] += value }

        fun flatten(prefix: String, value: Any?) {
            val key = prefix.ifBlank { "_" }
            when (value) {
                null -> put(abs(key.hashCode()), 0.1)
                is Number -> {
                    // simple numeric squash to [0,1]-ish
                    val d = value.toDouble()
                    val norm = 1.0 / (1.0 + kotlin.math.exp(-d / 100.0)) // rough sigmoid scaling
                    put(abs("$key:num".hashCode()), norm)
                }
                is Boolean -> put(abs("$key:bool".hashCode()), if (value) 1.0 else 0.0)
                is String -> {
                    // hashed representation of string token
                    put(abs("$key:str:${value}".hashCode()), 1.0)
                }
                is Map<*, *> -> value.forEach { (k, v2) ->
                    flatten(if (key == "_") k.toString() else "$key.$k", v2)
                }
                is Iterable<*> -> {
                    put(abs("$key:arr".hashCode()), (value as? Collection<*>)?.size?.toDouble() ?: 1.0)
                    value.forEachIndexed { i, el -> flatten("$key[$i]", el) }
                }
                else -> put(abs("$key:obj".hashCode()), 0.2)
            }
        }

        props.forEach { (k, v) -> flatten(k, v) }

        // normalize to [0,1] by simple max-abs (keeps it simple and stateless)
        val maxAbs = v.maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
        for (i in v.indices) v[i] = v[i] / maxAbs
        return v
    }

    private fun sampleBatch(): List<DoubleArray> =
        (1..batchSize).map {
            // approximate uniform sample
            val idx = (Math.random() * buffer.size).toInt().coerceIn(0, buffer.size - 1)
            buffer.elementAt(idx)
        }

    private fun toDataSet(batch: List<DoubleArray>): DataSet {
        val arr = Nd4j.create(batch.size, dim)
        for ((r, row) in batch.withIndex()) {
            for (c in 0 until dim) arr.putScalar(intArrayOf(r, c), row[c])
        }
        // Autoencoder trains to reconstruct X -> X
        return DataSet(arr, arr)
    }

    private fun mse(x: INDArray, y: INDArray): Double {
        val diff = x.sub(y)
        val sq = diff.mul(diff)
        return sq.meanNumber().toDouble()
    }

    fun isAnomalous(score: Double) = score > anomalyThreshold

}