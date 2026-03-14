package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew.Data

fun getMockDeviceDashboardViewState(): DeviceDashboardViewState {
    val dataPoints = mutableMapOf<Float, Data>()

    // Simulate a 20-second shot
    for (i in 0..200) {
        val seconds = i / 10f
        val pressure = when {
            seconds < 3f -> seconds * 2f // Pre-infusion ramp
            seconds < 25f -> 9f + (kotlin.math.sin(seconds) * 0.2f) // Main extraction at ~9 bar
            else -> 9f - (seconds - 25f) * 1.5f // Tapering off
        }.coerceAtLeast(0f)

        val flowRate = if (seconds < 3f) 0.5f else 2.2f + (kotlin.math.cos(seconds) * 0.1f)
        val weightRate = if (seconds < 5f) 0f else 2.8f + (kotlin.math.sin(seconds) * 0.7f)

        val volume = seconds * 1.8f
        val weight = if (seconds < 5f) 0f else (seconds - 5f) * 2.1f

        dataPoints[seconds] = Data(
            pressure = pressure,
            weight = weight,
            weightPerSecond = weightRate,
            volume = volume,
            volumePerSecond = flowRate
        )
    }

    return DeviceDashboardViewState(
        user = DeviceDashboardViewState.User(id = "1", name = "Barista Pro"),
        device = DeviceDashboardViewState.Device(
            id = "B0234556",
            name = "GeeFlow Pro",
            brewBoilerTemp = "93.5°",
            steamBoilerTemp = "125.0°",
            pressure = "9.0",
            connectionStatus = DeviceDashboardViewState.Device.ConnectionStatus.Connected,
            brewStatus = DeviceDashboardViewState.Device.BrewStatus.Manual
        ),
        brew = DeviceDashboardViewState.Brew(
            name = "Manual Extraction",
            time = 30f,
            data = dataPoints
        ),
        brewProfiles = listOf(
            DeviceDashboardViewState.Profile("Classic Espresso", "18g in, 36g out, 30s", true),
            DeviceDashboardViewState.Profile("Light Roast Filter", "High temp, low pressure", false)
        )
    )
}
