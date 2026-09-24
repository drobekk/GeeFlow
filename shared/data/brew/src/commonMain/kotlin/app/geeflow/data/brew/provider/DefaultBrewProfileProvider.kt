@file:Suppress("MagicNumber", "LongMethod")

package app.geeflow.data.brew.provider

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import org.koin.core.annotation.Factory

@Factory
class DefaultBrewProfileProvider {
    fun getDefaultProfiles(userId: Long): List<BrewProfile> = templates.map { it.copy(userId = userId) }

    private val templates = listOf(
        BrewProfile(
            userId = 0,
            name = "Cremina",
            description = "Lever-style: gentle preinfusion, 9 bar peak with declining pressure",
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
            position = 0,
        ),
        BrewProfile(
            userId = 0,
            name = "Blooming Espresso",
            description = "Flow preinfusion, 30s bloom pause, then 2.2 ml/s extraction",
            finishCondition = Condition.Weight(42f),
            steps = listOf(
                ProfileStep.Flow(time = 10, flow = 4f),
                ProfileStep.Wait(time = 30),
                ProfileStep.Flow(time = 25, flow = 2.2f),
            ),
            position = 1,
        ),
        BrewProfile(
            userId = 0,
            name = "Slayer Style",
            description = "Long low-flow preinfusion (1.5 ml/s) followed by 9 bar extraction",
            finishCondition = Condition.Weight(38f),
            steps = listOf(
                ProfileStep.Flow(time = 25, flow = 1.5f),
                ProfileStep.Pressure(time = 15, pressure = 9f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 2,
        ),
        BrewProfile(
            userId = 0,
            name = "Gentle & Sweet",
            description = "Low 6 bar peak declining to 4 bar, forgiving and smooth",
            finishCondition = Condition.Weight(36f),
            steps = listOf(
                ProfileStep.Pressure(time = 8, pressure = 2.5f),
                ProfileStep.Pressure(time = 12, pressure = 6f),
                ProfileStep.Pressure(time = 15, pressure = 4f),
            ),
            position = 3,
        ),
        BrewProfile(
            userId = 0,
            name = "Zuppa",
            description = "SOUP: low-pressure, high-flow extraction for sweetness & clarity, 20g in 60g out",
            finishCondition = Condition.Weight(60f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 13, flow = 6f),
            ),
            position = 4,
        ),
        BrewProfile(
            userId = 0,
            name = "Turbo Shot",
            description = "Fast 6 bar extraction for coarse grind, 15s shot",
            finishCondition = Condition.Weight(42f),
            steps = listOf(
                ProfileStep.Pressure(time = 3, pressure = 2.5f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 5,
        ),
        BrewProfile(
            userId = 0,
            name = "Londinium",
            description = "Spring lever: 3 bar infusion, 9.5 bar peak, declining to 4.5 bar",
            finishCondition = Condition.Weight(38f),
            steps = listOf(
                ProfileStep.Pressure(time = 6, pressure = 3f),
                ProfileStep.Pressure(time = 8, pressure = 9.5f),
                ProfileStep.Pressure(time = 8, pressure = 7.5f),
                ProfileStep.Pressure(time = 8, pressure = 6f),
                ProfileStep.Pressure(time = 8, pressure = 4.5f),
            ),
            position = 6,
        ),
        BrewProfile(
            userId = 0,
            name = "Rao Allongé",
            description = "High flow rate (4.5 ml/s) for light roasts, 90g out",
            finishCondition = Condition.Weight(90f),
            steps = listOf(
                ProfileStep.Flow(time = 6, flow = 3f),
                ProfileStep.Flow(time = 25, flow = 4.5f),
            ),
            position = 7,
        ),
        BrewProfile(
            userId = 0,
            name = "Filter 2.0",
            description = "Batch filter style: constant 3 ml/s flow through paper filter",
            finishCondition = Condition.Weight(100f),
            steps = listOf(
                ProfileStep.Flow(time = 40, flow = 3f),
            ),
            position = 8,
        ),
        BrewProfile(
            userId = 0,
            name = "Zuppa Lungo",
            description = "SOUP Lungo: extended low-pressure extraction for filter-like cup, 20g in 110g out",
            finishCondition = Condition.Weight(110f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 30, flow = 5f),
            ),
            position = 9,
        ),
        BrewProfile(
            userId = 0,
            name = "Espresso Lungo",
            description = "Long extraction for high yield, 80g out",
            finishCondition = Condition.Weight(80f),
            steps = listOf(
                ProfileStep.Pressure(time = 6, pressure = 3f),
                ProfileStep.Pressure(time = 25, pressure = 9f),
                ProfileStep.Pressure(time = 15, pressure = 6f),
            ),
            position = 10,
        ),
        BrewProfile(
            userId = 0,
            name = "Disco Italiano",
            description = "Traditional 9 bar extraction, 18g in 36g out",
            finishCondition = Condition.Weight(36f),
            steps = listOf(
                ProfileStep.Pressure(time = 30, pressure = 9f),
            ),
            position = 11,
        ),
    )
}
