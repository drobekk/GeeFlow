@file:Suppress("MagicNumber")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.User
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.HistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import kotlin.math.cos
import kotlin.math.sin

fun getMockDeviceDashboardViewState(): DeviceDashboardViewState {
    val dataPoints = mutableMapOf<Float, ChartData>()

    // Simulate a 20-second shot
    for (i in 0..200) {
        val seconds = i / 10f
        val pressure = when {
            seconds < 3f -> seconds * 2f // Pre-infusion ramp
            seconds < 25f -> 9f + (sin(seconds) * 0.2f) // Main extraction at ~9 bar
            else -> 9f - (seconds - 25f) * 1.5f // Tapering off
        }.coerceAtLeast(0f)

        val flowRate = if (seconds < 3f) 0.5f else 2.2f + (cos(seconds) * 0.1f)
        val weightRate = if (seconds < 5f) 0f else 2.8f + (sin(seconds) * 0.7f)

        val volume = seconds * 1.8f
        val weight = if (seconds < 5f) 0f else (seconds - 5f) * 2.1f

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
            name = "GeeFlow Pro",
            brewBoilerTemp = "93.5°",
            steamBoilerTemp = "125.0°",
            pressure = "9.0",
            connectionStatus = Device.ConnectionStatus.Connected,
            brewStatus = Device.BrewStatus.Manual,
        ),
        brew = Brew(
            name = "Manual Extraction",
            time = 30,
            data = dataPoints,
        ),
    )
}

fun getMockProfileListViewState(showHistory: Boolean = false) = ProfileListViewState(
    smartScaleConnected = true,
    showHistory = showHistory,
    history = List(12) { index ->
        HistoryBrew(
            id = index.toString(),
            badge = if (index % 3 == 0) "M" else "P",
            name = if (index % 3 == 0) "Manual brew" else "Light Roast",
            description = "2026-08-0${index % 9 + 1} 08:${30 + index} • ${26 + index} s",
            selected = index == 0,
        )
    },
    profiles = listOf(
        Profile(
            id = "1",
            number = "1",
            name = "Light Roast",
            description = "69g",
            brewByWeight = true,
            selected = true,
        ),
        Profile(
            id = "2",
            number = "2",
            name = "Dark Roast",
            description = "88ml",
            brewByWeight = false,
        ),
        Profile(
            id = "3",
            number = "3",
            name = "Turbo Shot",
            description = "36g",
            brewByWeight = true,
            bound = true,
        ),
    ) + List(10) { index ->
        Profile(
            id = (index + 4).toString(),
            number = (index + 4).toString(),
            name = "Profile ${index + 4}",
            description = if (index % 2 == 0) "${80 + index}ml" else "${40 + index}g",
            brewByWeight = index % 2 == 0,
        )
    },
)
