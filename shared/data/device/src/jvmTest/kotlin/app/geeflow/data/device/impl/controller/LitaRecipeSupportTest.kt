package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.provider.DefaultBrewProfileProvider
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.requiredMetrics
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LitaRecipeSupportTest {
    @Test
    fun `adaptive and londinium are app controlled and retain cup weight goals on LITA BA`() = runTest {
        val controller = WendougeeController(SupportedDevice.LitaBA, backgroundScope, mockk())
        val recipes = DefaultBrewProfileProvider().getDefaultProfiles(1)
            .filter { it.name in setOf("Adaptive v2", "Londinium") }

        assertEquals(setOf("Adaptive v2", "Londinium"), recipes.map { it.name }.toSet())
        recipes.forEach { profile ->
            assertEquals(ProfileExecution.AppControlled, controller.assessProfile(profile).execution, profile.name)
            assertTrue(profile.finishCondition is Condition.Weight, profile.name)
            assertTrue(BrewMetric.CupWeight in profile.requiredMetrics(), profile.name)
        }
    }
}
