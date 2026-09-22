package app.geeflow.presentation.feature.device.settings.maintenance

import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.MaintenanceSettings

internal fun MaintenanceSettingsViewState.toSettings() = MaintenanceSettings(
    daily = cleaning.toProgram(),
    deep = deepCleaning.toProgram(),
    dailyReminder = cleaning.reminder,
    deepReminder = deepCleaning.reminder,
)

private fun MaintenanceSettingsViewState.Cleaning.toProgram() = CleaningProgram(
    flushSeconds = timeSec.toInt(),
    restSeconds = restSec.toInt(),
    cycles = count.toInt(),
)
