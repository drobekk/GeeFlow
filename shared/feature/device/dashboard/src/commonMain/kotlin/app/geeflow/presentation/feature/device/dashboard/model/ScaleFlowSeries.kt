package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.domain.device.CupFlowEstimator

/** Uses the saved one-second regression when present and derives it for older recordings. */
internal fun oneSecondScaleFlowSeries(samples: List<BrewScaleSample>): List<Pair<BrewScaleSample, Float>> {
    val estimator = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
    return samples.asSequence()
        .filter { it.decoded }
        .sortedBy { it.elapsedMillis }
        .mapNotNull { sample ->
            val estimated = sample.weightGrams?.let { estimator.add(sample.elapsedMillis, it) }
            (sample.smoothedFlowGramsPerSecond ?: estimated)?.let { sample to it }
        }
        .toList()
}
