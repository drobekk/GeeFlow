package app.geeflow.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDashboard(val deviceId: Long) : NavKey
