package dev.drobek.geeflow.presentation.feature.device.settings.maintenance

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.core.presentation.launchCatching
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.SetCleaningSettingsUseCase
import dev.drobek.geeflow.domain.device.usecase.SetWaterAlarmUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.presentation.feature.device.settings.MaintenanceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.ApplyClicked
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningCountChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningRestChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningTimeChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.WaterAlarmToggled
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsViewModelEvent.ShowSnackbar
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_settings_applied
import geeflow.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class MaintenanceSettingsViewModel(
    @InjectedParam val arguments: MaintenanceSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val setCleaningSettings: SetCleaningSettingsUseCase,
    private val setWaterAlarm: SetWaterAlarmUseCase,
) : BaseViewModel<MaintenanceSettingsViewState, MaintenanceSettingsViewModelEvent>(MaintenanceSettingsViewState()) {

    private data class DeviceSnapshot(
        val cleaningTimeSec: String,
        val cleaningRestSec: String,
        val cleaningCount: String,
        val waterAlarm: Boolean,
    )

    private var deviceSnapshot: DeviceSnapshot? = null

    private var saveJob: Job? = null

    init {
        loadMachineState()
    }

    private fun loadMachineState() {
        launch {
            observeDeviceState(arguments.deviceId)
                .firstOrNull()
                ?.let(::updateViewState)
                ?: showError(IllegalStateException("Device not connected?"))
        }
    }

    private fun updateViewState(state: DeviceState) {
        val config = state.config
        modify {
            copy(
                cleaning = cleaning.copy(
                    timeSec = config?.cleaningTimeSec?.toInt()?.toString() ?: "1",
                    restSec = config?.cleaningStandbySec?.toInt()?.toString() ?: "1",
                    count = config?.cleaningCount?.toString() ?: "1"
                ),
                waterAlarm = config?.waterAlarmEnabled ?: false
            )
        }
        deviceSnapshot = snapshotFromState(viewState.value)
    }

    fun handleEvent(event: MaintenanceSettingsEvent) = when (event) {
        is CleaningTimeChanged -> modify { copy(cleaning = cleaning.copy(timeSec = event.timeSec)).withApplyVisible() }
        is CleaningRestChanged -> modify { copy(cleaning = cleaning.copy(restSec = event.standbySec)).withApplyVisible() }
        is CleaningCountChanged -> modify { copy(cleaning = cleaning.copy(count = event.count)).withApplyVisible() }
        is WaterAlarmToggled -> modify { copy(waterAlarm = event.enabled).withApplyVisible() }
        is ApplyClicked -> saveSettings()
        is CloseClicked -> {
            popTo(arguments, true)
            navigate(NavEvent.Back)
        }
    }

    private fun saveSettings() {
        saveJob?.cancel()
        saveJob = launchCatching(
            onError = ::showError,
            block = {
                modify { copy(applyButtonLoading = true) }
                with(viewState.value) {
                    setCleaningSettings(
                        deviceId = arguments.deviceId,
                        timeSec = cleaning.timeSec.toFloat(),
                        restSec = cleaning.restSec.toFloat(),
                        count = cleaning.count.toInt()
                    )
                    setWaterAlarm(arguments.deviceId, waterAlarm)
                }
                deviceSnapshot = snapshotFromState(viewState.value)
                modify { copy(applyButtonLoading = false, applyButtonVisible = false) }
                emitEvent(ShowSnackbar(getString(Res.string.common_settings_applied)))
            }
        )
    }

    private fun showError(throwable: Throwable) {
        Logger.e(throwable) { "Error while saving maintenance settings" }
        launch { emitEvent(ShowSnackbar(getString(Res.string.error_generic))) }
    }

    private fun snapshotFromState(state: MaintenanceSettingsViewState) = DeviceSnapshot(
        cleaningTimeSec = state.cleaning.timeSec,
        cleaningRestSec = state.cleaning.restSec,
        cleaningCount = state.cleaning.count,
        waterAlarm = state.waterAlarm,
    )

    private fun MaintenanceSettingsViewState.withApplyVisible(): MaintenanceSettingsViewState {
        val snapshot = deviceSnapshot ?: return this
        val changed = cleaning.timeSec != snapshot.cleaningTimeSec ||
                cleaning.restSec != snapshot.cleaningRestSec ||
                cleaning.count != snapshot.cleaningCount ||
                waterAlarm != snapshot.waterAlarm
        return copy(applyButtonVisible = changed)
    }
}
