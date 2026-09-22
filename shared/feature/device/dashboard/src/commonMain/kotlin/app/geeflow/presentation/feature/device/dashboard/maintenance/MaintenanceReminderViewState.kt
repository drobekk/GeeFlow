package app.geeflow.presentation.feature.device.dashboard.maintenance

import app.geeflow.data.device.model.CleaningType

internal data class MaintenanceReminderViewState(
    val dueTypes: Set<CleaningType> = emptySet(),
    val eligible: Boolean = false,
    val busy: Boolean = false,
)
