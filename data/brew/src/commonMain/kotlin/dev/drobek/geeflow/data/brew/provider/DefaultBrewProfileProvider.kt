@file:Suppress("MagicNumber")

package dev.drobek.geeflow.data.brew.provider

import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.data.brew.model.Condition
import dev.drobek.geeflow.data.brew.model.ProfileMode
import dev.drobek.geeflow.data.brew.model.ProfileStep
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
            name = "Zuppa",
            description = "20g in 60g out",
            mode = ProfileMode.VariablePressure,
            finishCondition = Condition.Weight(60f),
            steps = listOf(
                ProfileStep.Flow(time = 5, flow = 6f),
                ProfileStep.Wait(time = 2),
                ProfileStep.Flow(time = 13, flow = 6f),
            ),
            position = 1,
        ),
    )
}
