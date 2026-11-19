@file:Suppress("MagicNumber", "LongMethod")

package app.geeflow.data.brew.provider

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewSettings
import app.geeflow.data.brew.model.CaptureAction
import app.geeflow.data.brew.model.CaptureFilter
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExecutionPolicy
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.RecipeDefinition
import app.geeflow.data.brew.model.RecipeSource
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.SecondaryLimit
import app.geeflow.data.brew.model.TargetSource
import app.geeflow.data.brew.model.TargetSourceKind
import app.geeflow.data.brew.model.ThresholdComparison
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Factory

@Factory
class DefaultBrewProfileProvider {
    fun getDefaultProfiles(userId: Long): List<BrewProfile> = templates.map { it.copy(userId = userId) }

    fun standardizedDescription(name: String, description: String): String? {
        val canonicalName = when (name.trim().lowercase()) {
            "adaptive v2 (lita, no scale)" -> "Adaptive v2"
            else -> name.trim()
        }
        val template = templates.firstOrNull { it.name.equals(canonicalName, ignoreCase = true) } ?: return null
        if (template.name == "Adaptive v2" && description in adaptiveLegacyDescriptions) {
            return template.description
        }
        val legacyDescription = legacyDescriptions[template.name] ?: return null
        return template.description.takeIf { description == legacyDescription }
    }

    private val adaptivePhases = listOf(
        BrewPhase(
            id = "decent-1",
            name = "Prefill",
            control = PhaseControl.Flow(8f),
            maximumDurationMillis = 5_000,
        ),
        BrewPhase(
            id = "decent-2",
            name = "Fill",
            control = PhaseControl.Flow(8f),
            maximumDurationMillis = 12_000,
            exitConditions = listOf(ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.StrictlyAbove, 3f)),
        ),
        BrewPhase(
            id = "decent-3",
            name = "Compressing",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 12_000,
            exitConditions = listOf(ExitCondition(BrewMetric.PumpFlow, ThresholdComparison.StrictlyBelow, 3f)),
        ),
        BrewPhase(
            id = "decent-4",
            name = "Dripping",
            control = PhaseControl.Pressure(0.1f),
            maximumDurationMillis = 6_000,
        ),
        BrewPhase(
            id = "decent-5",
            name = "Pressurize",
            control = PhaseControl.Pressure(11f),
            maximumDurationMillis = 6_000,
            ramp = PhaseRamp(RampStyle.Linear, 6_000, RampStart.PreviousTarget),
            exitConditions = listOf(ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.StrictlyAbove, 8.8f)),
            exitCaptures = listOf(
                CaptureAction(
                    name = "extractionStartFlow",
                    metric = BrewMetric.PumpFlow,
                    windowMillis = 500,
                    maximumAgeMillis = 500,
                    filter = CaptureFilter.Mean,
                    minimum = 0f,
                    maximum = 12f,
                ),
            ),
            secondaryLimit = SecondaryLimit(metric = BrewMetric.PumpFlow, ceiling = 3.5f),
        ),
        BrewPhase(
            id = "decent-7",
            name = "Extraction",
            control = PhaseControl.Flow(3.5f),
            maximumDurationMillis = 60_000,
            ramp = PhaseRamp(
                style = RampStyle.Linear,
                durationMillis = 60_000,
                start = RampStart.PreviousTarget,
                source = TargetSource(TargetSourceKind.CapturedValue, capture = "extractionStartFlow"),
            ),
        ),
    )

    private val adaptiveRecipe = RecipeDefinition(
        templateId = "decent-6c359036f686a652",
        source = RecipeSource(
            format = "decent-json-v2",
            title = "Adaptive v2",
            author = "Decent",
            url = "https://github.com/decentespresso/decaid/blob/main/assets/defaultProfiles/best_practice.json",
            sha256 = "6c359036f686a652664e8a61ade092cb0993d65efc7c9f1a53561077a2fbb7a5",
            original = Json.parseToJsonElement(ADAPTIVE_SOURCE_JSON) as JsonObject,
        ),
        templatePhases = adaptivePhases,
        settings = BrewSettings(targetYieldG = 36f),
        diagnostics = listOf(
            "Original Decent temperature and limiter data is retained in the source JSON.",
            "GeeFlow captures pump flow at pressurization exit and uses it as the extraction ramp start.",
        ),
        adaptation = "adaptive-capture",
    )

    private val londiniumPhases = listOf(
        BrewPhase(id = "londinium-1", name = "Fill start", control = PhaseControl.Pressure(2f), maximumDurationMillis = 2_000),
        BrewPhase(
            id = "londinium-2",
            name = "Fill",
            control = PhaseControl.Pressure(2f),
            maximumDurationMillis = 10_000,
            exitConditions = listOf(ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.Above, 1.5f)),
        ),
        BrewPhase(id = "londinium-3", name = "Infuse", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10_000),
        BrewPhase(id = "londinium-4", name = "Pressure up", control = PhaseControl.Pressure(9f), maximumDurationMillis = 8_000),
        BrewPhase(
            id = "londinium-5",
            name = "Pressure decline",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 55_000,
            ramp = PhaseRamp(RampStyle.Linear, 55_000, RampStart.PreviousTarget),
            exitConditions = listOf(ExitCondition(BrewMetric.PumpFlow, ThresholdComparison.StrictlyAbove, 2.8f)),
        ),
        BrewPhase(
            id = "londinium-6",
            name = "Pressure hold",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 30_000,
            ramp = PhaseRamp(RampStyle.Linear, 30_000, RampStart.PreviousTarget),
            exitConditions = listOf(ExitCondition(BrewMetric.PumpFlow, ThresholdComparison.StrictlyAbove, 2.8f)),
        ),
        BrewPhase(
            id = "londinium-7",
            name = "Flow limit",
            control = PhaseControl.Flow(2.5f),
            maximumDurationMillis = 30_000,
            ramp = PhaseRamp(RampStyle.Linear, 30_000, RampStart.PreviousTarget),
        ),
    )

    private val templates = listOf(
        BrewProfile(
            userId = 0,
            name = "Adaptive v2",
            description = "For: Light to medium roast · Method: Flow prefill, pressure-triggered compression and adaptive flow · Aim: 36 g",
            finishCondition = Condition.Weight(36f),
            program = BrewProgram.Phases(
                phases = adaptivePhases,
                version = 2,
                execution = ExecutionPolicy(maximumTotalDurationMillis = 40_000),
                recipe = adaptiveRecipe,
            ),
            position = 0,
        ),
        BrewProfile(
            userId = 0,
            name = "Cremina",
            description = "For: Lever-style espresso · Method: 1.5 bar preinfusion, 9 bar peak, then taper · Aim: 38 g",
            finishCondition = Condition.Weight(38f),
            steps = listOf(
                ProfileStep.Pressure(time = 12, pressure = 1.5f),
                ProfileStep.Pressure(time = 8, pressure = 9f),
                ProfileStep.Pressure(time = 5, pressure = 8f),
                ProfileStep.Pressure(time = 5, pressure = 7f),
                ProfileStep.Pressure(time = 5, pressure = 6f),
                ProfileStep.Pressure(time = 5, pressure = 5f),
                ProfileStep.Pressure(time = 5, pressure = 4f),
                ProfileStep.Pressure(time = 5, pressure = 3f),
            ),
            position = 2,
        ),
        BrewProfile(
            userId = 0,
            name = "Blooming Espresso",
            description = "For: Sweet, lower-acidity espresso · Method: 4 ml/s preinfusion, 30 s pause, 2.2 ml/s brew · Aim: 42 g",
            finishCondition = Condition.Weight(42f),
            steps = listOf(
                ProfileStep.Flow(time = 10, flow = 4f),
                ProfileStep.Wait(time = 30),
                ProfileStep.Flow(time = 25, flow = 2.2f),
            ),
            position = 3,
        ),
        BrewProfile(
            userId = 0,
            name = "Slayer Style",
            description = "For: Low-flow preinfusion · Method: 1.5 ml/s, then 9 bar and 6 bar · Aim: 38 g",
            finishCondition = Condition.Weight(38f),
            steps = listOf(
                ProfileStep.Flow(time = 25, flow = 1.5f),
                ProfileStep.Pressure(time = 15, pressure = 9f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 4,
        ),
        BrewProfile(
            userId = 0,
            name = "Gentle & Sweet",
            description = "For: Milder espresso · Method: 2.5 bar start, 6 bar peak, taper to 4 bar · Aim: 36 g",
            finishCondition = Condition.Weight(36f),
            steps = listOf(
                ProfileStep.Pressure(time = 8, pressure = 2.5f),
                ProfileStep.Pressure(time = 12, pressure = 6f),
                ProfileStep.Pressure(time = 15, pressure = 4f),
            ),
            position = 5,
        ),
        BrewProfile(
            userId = 0,
            name = "Zuppa",
            description = "For: 1:3 espresso · Method: 6 ml/s flow with a 2 s pause · Aim: 20 g in, 60 g out",
            finishCondition = Condition.Weight(60f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 13, flow = 6f),
            ),
            position = 6,
        ),
        BrewProfile(
            userId = 0,
            name = "Turbo Shot",
            description = "For: Coarse-ground espresso · Method: 2.5 bar start, then 6 bar · Aim: 42 g",
            finishCondition = Condition.Weight(42f),
            steps = listOf(
                ProfileStep.Pressure(time = 3, pressure = 2.5f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 7,
        ),
        BrewProfile(
            userId = 0,
            name = "Londinium",
            description = "For: Spring-lever style · Method: 3 bar infusion, 9 bar peak, conditional taper and 2.5 ml/s finish · Aim: 38 g",
            finishCondition = Condition.Weight(38f),
            program = BrewProgram.Phases(
                phases = londiniumPhases,
                version = 2,
                execution = ExecutionPolicy(maximumTotalDurationMillis = 40_000),
            ),
            position = 1,
        ),
        BrewProfile(
            userId = 0,
            name = "Rao Allongé",
            description = "For: Light-roast allongé · Method: 3 then 4.5 ml/s flow · Aim: 90 g out",
            finishCondition = Condition.Weight(90f),
            steps = listOf(
                ProfileStep.Flow(time = 6, flow = 3f),
                ProfileStep.Flow(time = 25, flow = 4.5f),
            ),
            position = 8,
        ),
        BrewProfile(
            userId = 0,
            name = "Filter 2.0",
            description = "For: Filter-style cup · Method: Constant 3 ml/s flow · Aim: 100 g out",
            finishCondition = Condition.Weight(100f),
            steps = listOf(
                ProfileStep.Flow(time = 40, flow = 3f),
            ),
            position = 9,
        ),
        BrewProfile(
            userId = 0,
            name = "Zuppa Lungo",
            description = "For: Long, filter-like cup · Method: 6 ml/s flow, short pause, then 5 ml/s · Aim: 110 g out",
            finishCondition = Condition.Weight(110f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 30, flow = 5f),
            ),
            position = 10,
        ),
        BrewProfile(
            userId = 0,
            name = "Espresso Lungo",
            description = "For: Long espresso · Method: 3 bar start, 9 bar extraction, 6 bar finish · Aim: 80 g out",
            finishCondition = Condition.Weight(80f),
            steps = listOf(
                ProfileStep.Pressure(time = 6, pressure = 3f),
                ProfileStep.Pressure(time = 25, pressure = 9f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 11,
        ),
        BrewProfile(
            userId = 0,
            name = "Disco Italiano",
            description = "For: Traditional espresso · Method: Constant 9 bar pressure · Aim: 18 g in, 36 g out",
            finishCondition = Condition.Weight(36f),
            steps = listOf(
                ProfileStep.Pressure(time = 30, pressure = 9f),
            ),
            position = 12,
        ),
    )

    private companion object {
        private const val ADAPTIVE_SOURCE_JSON = """{"title":"Adaptive v2","author":"Decent","notes":"This profile aims to unite the best practices in espresso extraction that we have learned so far with the Decent. It unites Brakel's Londonium, Rao's Blooming and Gagné's Adaptive profiles. The flow rate during extraction will automatically adjust itself from the flow rate actually happening during the Pressurize step and then gently increase it from there.  A 15g dose is typical.  Aim for a flow rate around 1.5 ml/s at the Pressurize step. 15g in, 33g out, in 32 seconds is a typical recipe for a medium roast.  Grind fine enough to keep dripping during preinfusion to around 4g. If you have a bluetooth scale, you can keep track of how much dripping into the cup there is before the ramp stage.  4 grams of total dripping, within 1 gram, seems to give the best tasting results. Try adusting your grind and dose to get that amount of dripping, and see if you like how it tastes.","beverage_type":"espresso","steps":[{"name":"Prefill","temperature":"93.00","sensor":"coffee","pump":"flow","transition":"fast","pressure":"3.0","flow":"8.0","seconds":"5.00","volume":"100","weight":"0.0","limiter":{"value":"0","range":"0.6"}},{"name":"Fill","temperature":"93.00","sensor":"coffee","pump":"flow","transition":"fast","pressure":"3.0","flow":"8.0","seconds":"12.00","volume":"100","weight":"0.0","exit":{"type":"pressure","condition":"over","value":"3.00"},"limiter":{"value":"0","range":"0.6"}},{"name":"Compressing","temperature":"88.00","sensor":"coffee","pump":"pressure","transition":"fast","pressure":"3.0","flow":"6.8","seconds":"12.00","volume":"100","weight":"0","exit":{"type":"flow","condition":"under","value":"3.00"},"limiter":{"value":"0","range":"0.6"}},{"name":"Dripping","temperature":"88.00","sensor":"coffee","pump":"pressure","transition":"fast","pressure":"0.1","flow":"6.8","seconds":"6.00","volume":"0","weight":"0","limiter":{"value":"0","range":"0.6"}},{"name":"Pressurize","temperature":"88.00","sensor":"coffee","pump":"pressure","transition":"smooth","pressure":"11.0","flow":"","seconds":"6.00","volume":"100","weight":"0.0","exit":{"type":"pressure","condition":"over","value":"8.80"},"limiter":{"value":"3.5","range":"0.6"}},{"name":"Extraction start","temperature":"88.00","sensor":"coffee","pump":"flow","transition":"fast","pressure":"3.0","flow":"1.5","seconds":"0.00","volume":"0","weight":"0","exit":{"type":"pressure","condition":"over","value":"0"},"limiter":{"value":"9.5","range":"0.6"}},{"name":"Extraction","temperature":"88.00","sensor":"coffee","pump":"flow","transition":"smooth","pressure":"3.0","flow":"3.5","seconds":"60.00","volume":"100","weight":"0.0","limiter":{"value":"9.5","range":"0.6"}}],"tank_temperature":"0","target_weight":"36","target_volume":"36","target_volume_count_start":"3","legacy_profile_type":"settings_2c","type":"advanced","lang":"en","hidden":"0","reference_file":"best_practice","changes_since_last_espresso":"","version":"2"}"""

        val adaptiveLegacyDescriptions = setOf(
            "This profile aims to unite the best practices in espresso extraction that we have learned so far with the Decent. It unites Brakel's Londonium, Rao's Blooming and Gagné's Adaptive profiles. The flow rate during extraction will automatically adjust itself from the flow rate actually happening during the Pressurize step and then gently increase it from there.  A 15g dose is typical.  Aim for a flow rate around 1.5 ml/s at the Pressurize step. 15g in, 33g out, in 32 seconds is a typical recipe for a medium roast.  Grind fine enough to keep dripping during preinfusion to around 4g. If you have a bluetooth scale, you can keep track of how much dripping into the cup there is before the ramp stage.  4 grams of total dripping, within 1 gram, seems to give the best tasting results. Try adusting your grind and dose to get that amount of dripping, and see if you like how it tastes.",
            "Light to medium\n\nA 15g dose is typical.  Aim for a flow rate around 1.5 ml/s at the Pressurize step.\n5g in, 33g out, in 32 seconds is a typical recipe for a medium roast.  \nGrind fine enough to keep dripping during preinfusion to around 4g within 1 gram, seems to give the best tasting results",
        )

        val legacyDescriptions = mapOf(
            "Cremina" to "Lever-style: gentle preinfusion, 9 bar peak with declining pressure",
            "Blooming Espresso" to "Flow preinfusion, 30s bloom pause, then 2.2 ml/s extraction",
            "Slayer Style" to "Long low-flow preinfusion (1.5 ml/s) followed by 9 bar extraction",
            "Gentle & Sweet" to "Low 6 bar peak declining to 4 bar, forgiving and smooth",
            "Zuppa" to "SOUP: low-pressure, high-flow extraction for sweetness & clarity, 20g in 60g out",
            "Turbo Shot" to "Fast 6 bar extraction for coarse grind, 15s shot",
            "Londinium" to "Spring lever: 3 bar infusion, 9.5 bar peak, declining to 4.5 bar",
            "Rao Allongé" to "High flow rate (4.5 ml/s) for light roasts, 90g out",
            "Filter 2.0" to "Batch filter style: constant 3 ml/s flow through paper filter",
            "Zuppa Lungo" to "SOUP Lungo: extended low-pressure extraction for filter-like cup, 20g in 110g out",
            "Espresso Lungo" to "Long extraction for high yield, 80g out",
            "Disco Italiano" to "Traditional 9 bar extraction, 18g in 36g out",
        )
    }
}
