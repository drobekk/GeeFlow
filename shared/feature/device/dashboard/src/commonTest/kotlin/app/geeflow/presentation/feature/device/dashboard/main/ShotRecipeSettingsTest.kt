package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.BrewSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ShotRecipeSettingsTest {
    @Test
    fun yieldFollowsDoseAndRatio() {
        val settings = ShotRecipeSettings.forRecipe(18f, 36f)
        val doseChanged = settings.changeDose(0.1f)

        assertEquals(settings.ratio, doseChanged.ratio)
        assertEquals(36.2f, doseChanged.targetGrams)
        assertEquals(37.8f, settings.changeRatio(0.1f).targetGrams)
    }

    @Test
    fun editingYieldRetainsItsPrecisionAndUpdatesTheRatio() {
        val settings = ShotRecipeSettings.forRecipe(18f, 36f).changeTarget(1f)

        assertEquals(37f, settings.targetGrams)
        assertEquals(37f / 18f, settings.ratio)
        assertEquals(30.8f, settings.withDose(15f).targetGrams)
        assertEquals(true, settings.stopAtYield)
    }

    @Test
    fun yieldDefaultsToAnInformationalReferenceAndDoseDefaultsToEighteenGrams() {
        val settings = ShotRecipeSettings.forRecipe(null, 30f)

        assertEquals(18f, settings.doseGrams)
        assertEquals(30f, settings.targetGrams)
        assertEquals(false, settings.stopAtYield)
    }

    @Test
    fun ratioEditArmsYieldStopAndDoseEditKeepsItArmed() {
        val settings = ShotRecipeSettings.forRecipe(18f, 36f).changeRatio(0.1f)

        assertEquals(true, settings.stopAtYield)
        assertEquals(true, settings.withDose(15f).stopAtYield)
    }

    @Test
    fun weightCutoffUsesTheEarlierOfRecipeAndRequestedTargets() {
        assertEquals(30f, earlierWeightTarget(recipeTargetGrams = 36f, requestedTargetGrams = 30f))
        assertEquals(36f, earlierWeightTarget(recipeTargetGrams = 36f, requestedTargetGrams = 40f))
    }
    @Test
    fun historyUsesRecordedSetupAndDoesNotBorrowMismatchedRecipeDefaults() {
        val snapshot = BrewSettings(doseG = 18f, ratio = 2f, targetYieldG = 36f)
        assertEquals(Triple(19f, 2.1f, 39.9f), historicalRecipeValues(19f, 2.1f, 39.9f, snapshot))
        assertEquals(Triple(18f, 2f, 36f), historicalRecipeValues(null, null, null, snapshot))
        assertEquals(Triple(19f, null, null), historicalRecipeValues(19f, null, null, snapshot))
        assertEquals(Triple(18f, null, null), historicalRecipeValues(18f, null, null, null))
    }
}
