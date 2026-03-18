package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData

data class ProfileListViewState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: String? = null
) {
    data class Profile(
        val id: String,
        val number: String,
        val name: String,
        val description: String,
        val brewByWeight: Boolean,
        val bound: Boolean = false,
        val selected: Boolean = false,
        val targetData: Map<Float, ChartData> = emptyMap()
    )
}

