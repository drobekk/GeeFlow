@file:Suppress("MagicNumber")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.User
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileControlKind
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.HistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import app.geeflow.presentation.feature.device.dashboard.profiles.controlKind
import kotlin.math.sin

private val previewProfileProgram = BrewProgram.Phases(
    listOf(
        BrewPhase(
            id = "0",
            name = "Preinfusion",
            control = PhaseControl.Pressure(3f),
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "1",
            name = "Pause",
            control = PhaseControl.PumpPause,
            maximumDurationMillis = 5000,
        ),
        BrewPhase(
            id = "2",
            name = "Extraction",
            control = PhaseControl.Pressure(6f),
            maximumDurationMillis = 30000,
        ),
    ),
)

private val previewProgramsById = mapOf(
    "2" to previewProgram(PhaseControl.Flow(1.5f), PhaseControl.PumpPause, PhaseControl.Flow(2.2f)),
    "3" to previewProgram(PhaseControl.Flow(1.5f), PhaseControl.Pressure(9f)),
    "4" to previewProgram(PhaseControl.Pressure(6f), PhaseControl.Pressure(4f)),
    "5" to previewProgram(PhaseControl.Pressure(3f), PhaseControl.Flow(4f)),
    "6" to previewProgram(PhaseControl.Pressure(6f)),
    "7" to previewProgram(PhaseControl.Pressure(3f), PhaseControl.Pressure(9.5f), PhaseControl.Pressure(4.5f)),
    "8" to previewProgram(PhaseControl.Flow(4.5f)),
    "9" to previewProgram(PhaseControl.Flow(3f)),
    "10" to previewProgram(PhaseControl.Pressure(2f), PhaseControl.Flow(3f)),
    "11" to previewProgram(PhaseControl.Pressure(6f)),
    "12" to previewProgram(PhaseControl.Pressure(9f)),
    "13" to BrewProgram.Recording(FreeHandRecording(FreeHandControlMode.Flow, emptyList())),
    "14" to previewProgram(PhaseControl.PumpPause),
)

private fun previewProgram(vararg controls: PhaseControl) = BrewProgram.Phases(
    controls.mapIndexed { index, control ->
        BrewPhase(
            id = index.toString(),
            name = "Step ${index + 1}",
            control = control,
            maximumDurationMillis = 5_000,
        )
    },
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
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
            smartScaleConnected = true,
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
            description = "36g • 3 bar soak, 5s pause, then 6 bar extraction",
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
        Profile(
            id = "13",
            number = "13",
            name = "Recorded Cremina",
            description = "Recorded flow-controlled shot from history",
            brewByWeight = true,
            finishCondition = Condition.Weight(36f),
        ),
        Profile(
            id = "14",
            number = "14",
            name = "Pause-only soak",
            description = "A timed pause with no programmed pressure or flow target",
            brewByWeight = false,
        ),
    ).map { profile ->
        val program = profile.program ?: previewProgramsById[profile.id]
        profile.copy(
            program = program,
            controlKind = program?.controlKind() ?: ProfileControlKind.Other,
            targetData = if (program is BrewProgram.Phases) program.toTargetData() else profile.targetData,
        )
    },
)
