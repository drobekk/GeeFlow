package app.geeflow.presentation.feature.device.settings.maintenance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.CleaningReminder
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.SaveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.SetWaterAlarmUseCase
import app.geeflow.navigation.NavEvent
import co.touchlab.kermit.Logger
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_settings_applied
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import app.geeflow.presentation.feature.device.settings.MaintenanceSettings as MaintenanceDestination

@KoinViewModel
internal class MaintenanceSettingsViewModel(
    @InjectedParam val arguments: MaintenanceDestination,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val observeMaintenance: ObserveMaintenanceSettingsUseCase,
    private val saveMaintenance: SaveMaintenanceSettingsUseCase,
    private val setWaterAlarm: SetWaterAlarmUseCase,
) : BaseViewModel<MaintenanceSettingsViewState, MaintenanceSettingsViewModelEvent>(MaintenanceSettingsViewState()) {
    private var savedSettings: MaintenanceSettings? = null
    private var savedWaterAlarm = false

    init {
        launch {
            val limits = getDeviceConstraints(arguments.deviceId)
            val settings = observeMaintenance(arguments.deviceId).first()
            val state = observeDeviceState(arguments.deviceId).first { it.config != null }
            val template = MaintenanceSettingsViewState.Cleaning(
                timeList = limits.cleaningTimeRange.map { it.toString() },
                restList = limits.cleaningRestRange.map { it.toString() },
                countList = limits.cleaningCountRange.map { it.toString() },
            )
            savedSettings = settings
            savedWaterAlarm = state.config?.waterAlarmEnabled == true
            modify {
                copy(
                    cleaning = template.withProgram(settings.daily, settings.dailyReminder),
                    deepCleaning = template.withProgram(settings.deep, settings.deepReminder),
                    waterAlarm = savedWaterAlarm,
                )
            }
        }
    }

    fun handleEvent(event: MaintenanceSettingsEvent) {
        if (viewState.value.applyButtonLoading) return
        when (event) {
            is MaintenanceSettingsEvent.CleaningTimeChanged -> edit(event.type) { copy(timeSec = event.timeSec) }
            is MaintenanceSettingsEvent.CleaningRestChanged -> edit(event.type) { copy(restSec = event.standbySec) }
            is MaintenanceSettingsEvent.CleaningCountChanged -> edit(event.type) { copy(count = event.count) }
            is MaintenanceSettingsEvent.WaterAlarmToggled -> modify { copy(waterAlarm = event.enabled).withChanges() }
            is MaintenanceSettingsEvent.ReminderChanged -> edit(event.type) { copy(reminder = event.reminder) }
            MaintenanceSettingsEvent.ApplyClicked -> save()
            MaintenanceSettingsEvent.CloseClicked -> navigate(NavEvent.Back)
        }
    }

    private fun edit(
        type: CleaningType,
        block: MaintenanceSettingsViewState.Cleaning.() -> MaintenanceSettingsViewState.Cleaning,
    ) {
        modify {
            if (type == CleaningType.Daily) {
                copy(cleaning = cleaning.block()).withChanges()
            } else {
                copy(deepCleaning = deepCleaning.block()).withChanges()
            }
        }
    }

    private fun save() {
        if (savedSettings == null) return
        modify { copy(applyButtonLoading = true) }
        launchCatching(onError = { error ->
            Logger.e(error) { "Error saving maintenance settings" }
            modify { copy(applyButtonLoading = false) }
            launch { emitEvent(MaintenanceSettingsViewModelEvent.ShowSnackbar(getString(Res.string.error_generic))) }
        }) {
            val draft = viewState.value
            setWaterAlarm(arguments.deviceId, draft.waterAlarm)
            saveMaintenance(arguments.deviceId, draft.toSettings())
            savedSettings = draft.toSettings()
            savedWaterAlarm = draft.waterAlarm
            modify { copy(applyButtonLoading = false, applyButtonVisible = false) }
            emitEvent(MaintenanceSettingsViewModelEvent.ShowSnackbar(getString(Res.string.common_settings_applied)))
        }
    }

    private fun MaintenanceSettingsViewState.withChanges() = copy(
        applyButtonVisible = savedSettings != null && (toSettings() != savedSettings || waterAlarm != savedWaterAlarm),
    )

    private fun MaintenanceSettingsViewState.Cleaning.withProgram(program: CleaningProgram, reminder: CleaningReminder) = copy(
        timeSec = program.flushSeconds.toString(),
        restSec = program.restSeconds.toString(),
        count = program.cycles.toString(),
        reminder = reminder,
    )
}
