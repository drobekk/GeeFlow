package app.geeflow.data.device

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfilingCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileSupportTest {
    private val demo = DemoDeviceController(CoroutineScope(Dispatchers.Default))
    private val recipe =
        BrewProfile(
            userId = 1,
            name = "Portable",
            description = "",
            program = BrewProgram.Phases(
                listOf(
                    BrewPhase(
                        "infusion",
                        control = PhaseControl.Pressure(3f),
                        maximumDurationMillis = 10000,
                        exitConditions = listOf(ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 40f))
                    )
                )
            )
        )

    @Test fun samePortableRecipeCanBeNativeAppControlledOrUnsupported() {
        val native = object : DeviceController by demo {
            override fun assessNativeProfile(profile: BrewProfile): List<ProfileIssue> = emptyList()
        }
        val unsupported = object : DeviceController by demo {
            override val profilingCapabilities = ProfilingCapabilities()
        }
        assertEquals(ProfileExecution.Native, native.assessProfile(recipe).execution)
        assertTrue(native.assessProfile(recipe).bindingAllowed)
        assertEquals(ProfileExecution.AppControlled, demo.assessProfile(recipe).execution)
        assertFalse(demo.assessProfile(recipe).bindingAllowed)
        assertEquals(ProfileExecution.Unsupported, unsupported.assessProfile(recipe).execution)
    }

    @Test fun disconnectedScaleDoesNotEraseSupportedCapability() {
        val weighted = recipe.copy(
            program = BrewProgram.Phases(
                listOf(
                    (recipe.program as BrewProgram.Phases).phases.first().copy(
                        exitConditions = listOf(
                            ExitCondition(BrewMetric.CupWeight, ThresholdComparison.Above, 36f)
                        )
                    )
                )
            )
        )
        assertEquals(ProfileExecution.AppControlled, demo.assessProfile(weighted).execution)
        assertTrue(demo.assessProfile(weighted, checkAvailability = true).issues.isNotEmpty())
    }

    @Test fun unknownLiveModeSwitchIsRejectedWithoutDroppingConditions() {
        val phases = (recipe.program as BrewProgram.Phases).phases
        val mixed = recipe.copy(
            program = BrewProgram.Phases(phases + phases.first().copy(id = "flow", control = PhaseControl.Flow(4f)))
        )
        assertEquals(ProfileExecution.Unsupported, demo.assessProfile(mixed).execution)
    }
}
