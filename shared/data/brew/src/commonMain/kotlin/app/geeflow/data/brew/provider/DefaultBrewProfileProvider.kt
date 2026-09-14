@file:Suppress("MagicNumber", "LongMethod")

package app.geeflow.data.brew.provider

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileMode
import app.geeflow.data.brew.model.ProfileStep
import org.koin.core.annotation.Factory

@Factory
class DefaultBrewProfileProvider {
    fun getDefaultProfiles(userId: Long): List<BrewProfile> = listOf(
        BrewProfile(
            userId = userId,
            name = "Disco Italiano",
            description = "Traditional 9 bar extraction",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Volume(50f),
            steps = listOf(
                ProfileStep.Pressure(time = 30, pressure = 9f),
            ),
            position = 0,
        ),
        BrewProfile(
            userId = userId,
            name = "Disco Italiano Lungo",
            description = "Traditional 9 bar extraction, 100ml out",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Volume(100f),
            steps = listOf(
                ProfileStep.Pressure(time = 30, pressure = 9f),
            ),
            position = 1,
        ),
        BrewProfile(
            userId = userId,
            name = "Zuppa",
            description = "20g in 60g out",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Weight(60f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 13, flow = 6f),
            ),
            position = 2,
        ),
        BrewProfile(
            userId = userId,
            name = "Zuppa Lungo",
            description = "20g in 110g out",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Weight(110f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 30, flow = 5f),
            ),
            position = 3,
        ),
        BrewProfile(
            userId = userId,
            name = "Cremina",
            description = "Lever-style: long soak, 9 bar peak, declining to 3 bar",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Weight(38f),
            steps = listOf(
                ProfileStep.Pressure(time = 15, pressure = 1.1f),
                ProfileStep.Pressure(time = 10, pressure = 9f),
                ProfileStep.Pressure(time = 6, pressure = 8f),
                ProfileStep.Pressure(time = 6, pressure = 7f),
                ProfileStep.Pressure(time = 6, pressure = 6f),
                ProfileStep.Pressure(time = 6, pressure = 5f),
                ProfileStep.Pressure(time = 6, pressure = 4f),
                ProfileStep.Pressure(time = 5, pressure = 3f),
            ),
            position = 4,
        ),
        BrewProfile(
            userId = userId,
            name = "Blooming Espresso",
            description = "Flow preinfusion, 30s bloom, then 2.2 ml/s extraction",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Weight(42f),
            steps = listOf(
                ProfileStep.Flow(time = 25, flow = 4f),
                ProfileStep.Wait(time = 30),
                ProfileStep.Flow(time = 25, flow = 2.2f),
            ),
            position = 5,
        ),
        BrewProfile(
            userId = userId,
            name = "Preinfusion",
            description = "18g in 36g out, 9 bar after a 5s soak",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Volume(78f),
            steps = listOf(
                ProfileStep.Pressure(time = 5, pressure = 3f),
                ProfileStep.Wait(time = 5),
                ProfileStep.Pressure(time = 35, pressure = 9f),
            ),
            position = 6,
        )
    )
}
