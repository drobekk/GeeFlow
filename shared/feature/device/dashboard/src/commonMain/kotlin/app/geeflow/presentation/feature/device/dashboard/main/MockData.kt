@file:Suppress("MagicNumber")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.User
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.HistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import kotlin.math.sin

private val previewProfileProgram = BrewProgram.Phases(
    listOf(
        BrewPhase(
            id = "0",
            name = "Preinfusion",
            control = PhaseControl.Pressure(1.5f),
            maximumDurationMillis = 12000,
        ),
        BrewPhase(
            id = "1",
            name = "Peak",
            control = PhaseControl.Pressure(9f),
            maximumDurationMillis = 8000,
        ),
        BrewPhase(
            id = "2",
            name = "Decline 8",
            control = PhaseControl.Pressure(8f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "3",
            name = "Decline 7",
            control = PhaseControl.Pressure(7f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "4",
            name = "Decline 6",
            control = PhaseControl.Pressure(6f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "5",
            name = "Decline 5",
            control = PhaseControl.Pressure(5f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "6",
            name = "Decline 4",
            control = PhaseControl.Pressure(4f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "7",
            name = "Decline 3",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 5000,
        ),
    ),
)

@Suppress("CyclomaticComplexMethod")
fun getMockDeviceDashboardViewState(): DeviceDashboardViewState {
    val dataPoints = mutableMapOf<Float, ChartData>()
    var volume = 0f
    var weight = 0f

    // Measured response trails the programmed targets and varies slightly during extraction.
    for (i in 0..400) {
        val seconds = i / 10f
        val pressure = when {
            seconds < 2.3f -> smoothRamp(seconds, 0f, 2.3f, 0f, 2.85f)
            seconds < 5f -> 2.85f + 0.08f * sin(seconds * 1.3f)
            seconds < 7.2f -> smoothRamp(seconds, 5f, 7.2f, 2.85f, 0.25f)
            seconds < 10f -> 0.25f + 0.04f * sin(seconds * 1.7f)
            seconds < 15f -> smoothRamp(seconds, 10f, 15f, 0.25f, 6.15f)
            seconds < 28f -> 6.1f + 0.13f * sin(seconds * 0.8f) + 0.05f * sin(seconds * 2.1f)
            else -> smoothRamp(seconds, 28f, 40f, 6.0f, 5.5f) +
                0.1f * sin(seconds * 0.8f) + 0.05f * sin(seconds * 2.1f)
        }
        val flowRate = when {
            seconds < 5f -> smoothRamp(seconds, 0f, 5f, 1f, 0.45f)
            seconds < 7.5f -> smoothRamp(seconds, 5f, 7.5f, 0.45f, 0.02f)
            seconds < 10f -> 0.02f
            seconds < 16f -> smoothRamp(seconds, 10f, 16f, 0.05f, 1.1f)
            else -> smoothRamp(seconds, 16f, 40f, 1.1f, 2.1f) + 0.05f * sin(seconds * 1.35f)
        }
        val weightRate = when {
            seconds < 13f -> 0f
            seconds < 19f -> smoothRamp(seconds, 13f, 19f, 0f, 1f)
            else -> smoothRamp(seconds, 19f, 40f, 1f, 2.2f) + 0.07f * sin(seconds * 1.8f)
        }
        if (i > 0) {
            volume += flowRate * 0.1f
            weight += weightRate * 0.1f
        }

        dataPoints[seconds] = ChartData(
            pressure = pressure,
            weight = weight,
            weightPerSecond = weightRate,
            volume = volume,
            volumePerSecond = flowRate,
        )
    }

    return DeviceDashboardViewState(
        user = User(id = "1", name = "Barista Pro"),
        device = Device(
            id = 0L,
            name = "Data-S",
            brewBoilerTemp = "93.5°",
            steamBoilerTemp = "125.0°",
            pressure = "5.5",
            connectionStatus = Device.ConnectionStatus.Connected,
            brewStatus = Device.BrewStatus.Profile,
        ),
        brew = Brew(
            name = "Cremina",
            description = "18g in · 38g out",
            time = 50,
            data = dataPoints,
        ),
    )
}

private fun smoothRamp(time: Float, start: Float, end: Float, from: Float, to: Float): Float {
    val progress = ((time - start) / (end - start)).coerceIn(0f, 1f)
    val eased = progress * progress * (3f - 2f * progress)
    return from + (to - from) * eased
}

@Suppress("LongMethod")
fun getMockProfileListViewState(showHistory: Boolean = false) = ProfileListViewState(
    smartScaleConnected = true,
    showHistory = showHistory,
    history = List(12) { index ->
        HistoryBrew(
            id = index.toString(),
            name = if (index % 3 == 0) {
                "Manual brew"
            } else if (index % 2 == 0) {
                "Cremina"
            } else {
                "Zuppa"
            },
            description = "2026-08-0${index % 9 + 1} 08:${30 + index} • ${26 + index} s",
            selected = index == 0,
        )
    },
    profiles = listOf(
        Profile(
            id = "1",
            number = "1",
            name = "Cremina",
            description = "38g • Lever-style: gentle preinfusion, 9 bar peak with declining pressure",
            brewByWeight = true,
            selected = true,
            experimental = false,
            program = previewProfileProgram,
            finishCondition = Condition.Weight(38f),
            targetData = previewProfileProgram.toTargetData(),
        ),
        Profile(
            id = "2",
            number = "2",
            name = "Blooming Espresso",
            description = "42g • Flow preinfusion, 30s bloom pause, then 2.2 ml/s extraction",
            brewByWeight = true,
        ),
        Profile(
            id = "3",
            number = "3",
            name = "Slayer Style",
            description = "38g • Long low-flow preinfusion (1.5 ml/s) followed by 9 bar extraction",
            brewByWeight = true,
        ),
        Profile(
            id = "4",
            number = "4",
            name = "Gentle & Sweet",
            description = "36g • Low 6 bar peak declining to 4 bar, forgiving and smooth",
            brewByWeight = true,
        ),
        Profile(
            id = "5",
            number = "5",
            name = "Zuppa",
            description = "60g • SOUP: low-pressure, high-flow extraction for sweetness & clarity, 20g in 60g out",
            brewByWeight = true,
        ),
        Profile(
            id = "6",
            number = "6",
            name = "Turbo Shot",
            description = "42g • Fast 6 bar extraction for coarse grind, 15s shot",
            brewByWeight = true,
        ),
        Profile(
            id = "7",
            number = "7",
            name = "Londinium",
            description = "38g • Spring lever: 3 bar infusion, 9.5 bar peak, declining to 4.5 bar",
            brewByWeight = true,
        ),
        Profile(
            id = "8",
            number = "8",
            name = "Rao Allongé",
            description = "90g • High flow rate (4.5 ml/s) for light roasts, 90g out",
            brewByWeight = true,
        ),
        Profile(
            id = "9",
            number = "9",
            name = "Filter 2.0",
            description = "100g • Batch filter style: constant 3 ml/s flow through paper filter",
            brewByWeight = true,
        ),
        Profile(
            id = "10",
            number = "10",
            name = "Zuppa Lungo",
            description = "110g • SOUP Lungo: extended low-pressure extraction for filter-like cup, 20g in 110g out",
            brewByWeight = true,
        ),
        Profile(
            id = "11",
            number = "11",
            name = "Espresso Lungo",
            description = "80g • Long extraction for high yield, 80g out",
            brewByWeight = true,
        ),
        Profile(
            id = "12",
            number = "12",
            name = "Disco Italiano",
            description = "36g • Traditional 9 bar extraction, 18g in 36g out",
            brewByWeight = true,
            bound = true,
        ),
    ),
)
