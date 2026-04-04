package dev.drobek.geeflow.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDashboard(val deviceId: String) : NavKey
