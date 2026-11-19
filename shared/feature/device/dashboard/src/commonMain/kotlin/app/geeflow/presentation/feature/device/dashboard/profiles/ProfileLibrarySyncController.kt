package app.geeflow.presentation.feature.device.dashboard.profiles

import kotlinx.coroutines.flow.StateFlow

/** App-level profile library controls exposed to the profile list. */
interface ProfileLibrarySyncController {
    val state: StateFlow<ProfileLibrarySyncState>
    suspend fun syncNow()
}

data class ProfileLibrarySyncState(
    val phase: Phase = Phase.NotConfigured,
    val message: String? = null,
    val lastSyncedAt: String? = null,
) {
    enum class Phase { NotConfigured, Ready, Syncing, Synced, Offline, Error }
}
