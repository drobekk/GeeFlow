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
            id = "preinfusion",
            name = "Preinfusion",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "pause",
            name = "Pause",
            control = PhaseControl.PumpPause,
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "extraction",
            name = "Extraction",
            control = PhaseControl.Pressure(6f),
            maximumDurationMillis = 30000,
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
            name = "Preinfusion",
            description = "18g in · 36g out",
            time = 40,
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
            badge = if (index % 3 == 0) {
                "M"
            } else {
                "P"
            },
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
            name = "Preinfusion",
            description = "36g · 40s profile",
            brewByWeight = true,
            selected = true,
            experimental = false,
            program = previewProfileProgram,
            finishCondition = Condition.Weight(36f),
            targetData = previewProfileProgram.toTargetData(),
        ),
        Profile(
            id = "2",
            number = "2",
            name = "Disco Italiano",
            description = "50ml · 30s profile",
            brewByWeight = false,
        ),
        Profile(
            id = "3",
            number = "3",
            name = "Disco Italiano Lungo",
            description = "100ml · 30s profile",
            brewByWeight = false,
            bound = true,
        ),
        Profile(
            id = "4",
            number = "4",
            name = "Zuppa",
            description = "60g · 20s profile",
            brewByWeight = true,
        ),
        Profile(
            id = "5",
            number = "5",
            name = "Zuppa Lungo",
            description = "110g · 37s profile",
            brewByWeight = true,
        ),
        Profile(
            id = "6",
            number = "6",
            name = "Cremina",
            description = "38g · 60s profile",
            brewByWeight = true,
        ),
        Profile(
            id = "7",
            number = "7",
            name = "Blooming Espresso",
            description = "42g · 80s profile",
            brewByWeight = true,
        ),
    ),
)
