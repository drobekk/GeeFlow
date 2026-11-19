package app.geeflow.presentation.feature.device.dashboard.main

import kotlin.math.min
import kotlin.math.round

data class ShotRecipeSettings(
    val doseGrams: Float,
    val ratio: Float,
    val stopAtYield: Boolean = false,
) {
    val targetGrams: Float get() = (doseGrams * ratio).roundToTenth().coerceAtLeast(0.1f)
    val machineStopGrams: Float get() = targetGrams.toInt().coerceAtLeast(1).toFloat()

    fun changeDose(delta: Float): ShotRecipeSettings = withDose(doseGrams + delta)

    fun withDose(dose: Float): ShotRecipeSettings = copy(doseGrams = dose.coerceAtLeast(0.1f).roundToTenth())

    fun changeRatio(delta: Float): ShotRecipeSettings {
        val ratio = (ratio + delta).coerceAtLeast(0.1f)
        return copy(ratio = ratio, stopAtYield = true)
    }

    fun changeTarget(delta: Float): ShotRecipeSettings {
        val target = (targetGrams + delta).coerceAtLeast(0.1f).roundToTenth()
        return copy(ratio = target / doseGrams, stopAtYield = true)
    }

    companion object {
        fun forRecipe(doseGrams: Float?, targetGrams: Float): ShotRecipeSettings {
            val target = targetGrams.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_TARGET_GRAMS
            val dose = doseGrams?.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_DOSE_GRAMS
            return ShotRecipeSettings(dose, target / dose)
        }

        const val DEFAULT_DOSE_GRAMS = 18f
        const val DEFAULT_TARGET_GRAMS = 36f
    }
}

internal fun earlierWeightTarget(recipeTargetGrams: Float, requestedTargetGrams: Float): Float =
    min(recipeTargetGrams, requestedTargetGrams)

private fun Float.roundToTenth() = round(this * 10f) / 10f

/** Only use setup values recorded for this shot or compatible values from its recipe snapshot. */
internal fun historicalRecipeValues(
    recordedDose: Float?,
    recordedRatio: Float?,
    recordedTarget: Float?,
    recipe: app.geeflow.data.brew.model.BrewSettings?,
): Triple<Float?, Float?, Float?> {
    fun valid(value: Float?) = value?.takeIf { it.isFinite() && it > 0f }
    val dose = valid(recordedDose) ?: valid(recipe?.doseG)
    val snapshotMatchesDose = recordedDose == null || recipe?.doseG?.let {
        kotlin.math.abs(it - recordedDose) < 0.001f
    } == true
    val target = valid(recordedTarget) ?: valid(recipe?.effectiveYieldG).takeIf { snapshotMatchesDose }
    val ratio = valid(recordedRatio) ?: valid(recipe?.ratio).takeIf { snapshotMatchesDose }
        ?: if (target != null && dose != null) target / dose else null
    return Triple(dose, ratio, target)
}
