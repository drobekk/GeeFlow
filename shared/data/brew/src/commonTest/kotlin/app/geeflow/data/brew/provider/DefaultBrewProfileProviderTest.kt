package app.geeflow.data.brew.provider

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ThresholdComparison
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultBrewProfileProviderTest {
    private val provider = DefaultBrewProfileProvider()

    @Test
    fun `adaptive v2 keeps only its flow limiter and cup weight goal`() {
        val profile = provider.getDefaultProfiles(1).single { it.name == "Adaptive v2" }
        val program = assertIsPhases(profile.program)

        assertEquals(2, program.version)
        assertEquals(40_000L, program.execution.maximumTotalDurationMillis)
        assertEquals(Condition.Weight(36f), profile.finishCondition)
        assertTrue(program.phases.all { it.temperatureC == null })
        assertTrue(program.phases.none { it.secondaryLimit?.let { limit ->
            limit.metric == BrewMetric.PumpPressure && limit.ceiling == 9.5f
        } == true })
        assertEquals(3.5f, program.phases.single { it.name == "Pressurize" }.secondaryLimit?.ceiling)
        assertEquals(BrewMetric.PumpFlow, program.phases.single { it.name == "Pressurize" }.secondaryLimit?.metric)
        assertEquals(PhaseControl.Flow(3.5f), program.phases.last().control)
        assertTrue(profile.description.contains("Aim: 36 g"))
    }

    @Test
    fun `londinium matches editor phases and weight target`() {
        val profile = provider.getDefaultProfiles(1).single { it.name == "Londinium" }
        val program = assertIsPhases(profile.program)

        assertEquals(2, program.version)
        assertEquals(40_000L, program.execution.maximumTotalDurationMillis)
        assertEquals(Condition.Weight(38f), profile.finishCondition)
        assertEquals(7, program.phases.size)
        assertEquals(PhaseControl.Pressure(9f), program.phases[3].control)
        assertEquals(
            BrewMetric.PumpFlow to ThresholdComparison.StrictlyAbove,
            program.phases[4].exitConditions.single().let { it.metric to it.comparison },
        )
        assertEquals(2.8f, program.phases[4].exitConditions.single().threshold)
        assertEquals(PhaseControl.Flow(2.5f), program.phases.last().control)
        assertTrue(profile.description.contains("9 bar peak"))
        assertFalse(profile.description.contains("9.5 bar"))
    }

    @Test
    fun `editor recipe metadata survives program serialization`() {
        val profile = provider.getDefaultProfiles(1).single { it.name == "Adaptive v2" }
        val program = assertIsPhases(profile.program)
        val json = Json { encodeDefaults = true }

        assertEquals(program, json.decodeFromString<BrewProgram>(json.encodeToString<BrewProgram>(program)))
        val recipe = assertNotNull(program.recipe)
        assertEquals(36f, recipe.settings.targetYieldG)
    }

    private fun assertIsPhases(program: BrewProgram): BrewProgram.Phases =
        (program as? BrewProgram.Phases) ?: error("Expected a phase recipe")
}
