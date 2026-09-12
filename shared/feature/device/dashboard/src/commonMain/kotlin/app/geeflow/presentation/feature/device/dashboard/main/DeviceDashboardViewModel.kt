@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.isDemo
import app.geeflow.data.user.model.ChartType
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.brew.usecase.SaveBrewToHistoryUseCase
import app.geeflow.domain.device.usecase.ConnectDeviceUseCase
import app.geeflow.domain.device.usecase.DisconnectDeviceUseCase
import app.geeflow.domain.device.usecase.GetDeviceUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import app.geeflow.domain.device.usecase.StartManualBrewingUseCase
import app.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import app.geeflow.domain.device.usecase.StopBrewingUseCase
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.GetSkipManualBrewHistoryUseCase
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.navigation.destination.FreeControl
import app.geeflow.navigation.destination.UserSettings
import app.geeflow.platform.permissions.DeniedException
import app.geeflow.platform.permissions.PermissionBluetoothConnect
import app.geeflow.platform.permissions.PermissionBluetoothScan
import app.geeflow.platform.permissions.PermissionsController
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import app.geeflow.presentation.feature.device.dashboard.QuickSettings
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.AlarmClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.BrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.BrewDescriptionClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.CleaningClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectedDevicesClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectionButtonClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DeviceClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.EditProfileClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.FlowControlClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.HistoryBrewSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ManualBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.OpenSystemSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.PermissionDialogResumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ProfileSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsLongPressed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.Resumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.StopBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Dialog
import app.geeflow.presentation.feature.device.dashboard.model.toChartData
import app.geeflow.presentation.feature.device.dashboard.model.toDashboard
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_steam_boiler_off
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_steam_boiler_on
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.math.pow
import kotlin.math.round
import app.geeflow.data.device.model.Device as Machine

@KoinViewModel
internal class DeviceDashboardViewModel(
    @InjectedParam private val args: DeviceDashboard,
    @InjectedParam val permissionsController: PermissionsController,
    getDevice: GetDeviceUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val startManualBrewing: StartManualBrewingUseCase,
    private val startProfileBrewing: StartProfileBrewingUseCase,
    private val stopBrewing: StopBrewingUseCase,
    private val observeBrewData: ObserveBrewDataUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val getBrewProfileUseCase: GetBrewProfileUseCase,
    private val getSelectedUser: GetSelectedUserUseCase,
    private val saveBrewToHistory: SaveBrewToHistoryUseCase,
    private val getSkipManualBrewHistory: GetSkipManualBrewHistoryUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase,
) : BaseViewModel<DeviceDashboardViewState, DeviceDashboardViewModelEvent>(DeviceDashboardViewState()) {

    private var selectedProfileId: String? = null
    private var selectedProfileName: String? = null
    private var selectedProfileDescription: String? = null
    private var selectedProfileSteps: List<ProfileStep> = emptyList()
    private var selectedProfileRecording: FreeHandRecording? = null
    private var machine: Machine? = null
    private var deviceConfig: DeviceState.Config? = null
    private var brewInProgress = false
    private var skipManualBrews = true

    init {
        machine = getDevice(args.deviceId)
        modify { copy(device = device.copy(id = args.deviceId, name = machine?.name ?: args.deviceId.toString())) }
        launch { observeDeviceState(args.deviceId).collect { state -> updateMachineStateUi(state) } }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
        launch { observeBrewData(args.deviceId).collect(::brewSessionDataChanged) }
        launch { getSelectedUser().collect { u -> modify { copy(user = user.copy(photoFileName = u?.photoUri)) } } }
        launch { getSkipManualBrewHistory().collect { skipManualBrews = it } }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> navigate(To(DeviceList))
        is QuickSettingsClicked -> withDeviceConnected { navigate(To(QuickSettings(args.deviceId))) }
        is QuickSettingsLongPressed -> withDeviceConnected { toggleSteamBoiler() }
        is UserClicked -> navigate(To(UserSettings))
        is ConnectedDevicesClicked -> withDeviceConnected {
            navigate(To(DeviceSettings(args.deviceId, EntryPoint.Connectivity)))
        }

        is CleaningClicked -> withDeviceConnected { navigate(To(QuickMaintenance(args.deviceId))) }
        is BrewDescriptionClicked -> modify {
            copy(dialog = Dialog.BrewDescription(brew.name, brew.description))
        }

        is EditProfileClicked -> editSelectedProfile()
        is DialogDismissed -> modify { copy(dialog = null) }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is ManualBrewClicked -> launchCatching(::onError) { startManualBrewing(args.deviceId) }
        is StopBrewClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is FlowControlClicked -> navigate(To(FreeControl(args.deviceId)))
        is BrewClicked -> startProfile()
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is ProfileSelected -> onProfileSelected(event.id)
        is HistoryBrewSelected -> modify {
            copy(
                brew = Brew(
                    name = event.name,
                    time = event.durationSeconds,
                    data = event.data,
                    historyTarget = event.targetData,
                    phaseTransitions = event.phaseTransitions,
                ),
            )
        }

        is Resumed -> connect()
        is AlarmClicked -> withDeviceConnected { navigate(To(QuickMaintenance(args.deviceId))) }
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            disconnectDevice(args.deviceId)
        } else {
            withBluetoothPermissions {
                connectDevice(args.deviceId)
            }
        }
    }

    private fun connect() {
        withBluetoothPermissions {
            if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Disconnected) {
                connectDevice(args.deviceId)
            }
        }
    }

    private fun editSelectedProfile() {
        val profileId = selectedProfileId?.toLongOrNull() ?: return
        modify { copy(dialog = null) }
        navigate(To(ProfileEditor(args.deviceId, profileId)))
    }

    private fun startProfile() = launchCatching(::onError) {
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { profile ->
                try {
                    modify { copy(brewButtonState = BrewButtonState.Syncing) }
                    startProfileBrewing(args.deviceId, profile)
                } finally {
                    modify {
                        copy(
                            brewButtonState = if (device.isBrewing) {
                                BrewButtonState.Brewing
                            } else {
                                BrewButtonState.Idle
                            },
                        )
                    }
                }
                emitEvent(DeviceDashboardViewModelEvent.SwitchToDetails)
            }
    }

    private fun onProfileSelected(id: String?) {
        selectedProfileId = id
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let {
                selectedProfileName = it.name
                selectedProfileDescription = it.description
                selectedProfileSteps = it.steps
                selectedProfileRecording = it.recording
                modify { copy(brew = Brew(name = it.name, description = it.description)) }
            }
    }

    private fun updateMachineStateUi(state: DeviceState) {
        deviceConfig = state.config
        val wasConnected = viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected
        val isNowConnected = state.connectionStatus == ConnectionStatus.Connected
        if (!wasConnected && isNowConnected && state.waterLevelAlarm) {
            navigate(To(QuickMaintenance(args.deviceId)))
        }
        modify {
            val newBrewStatus = when (state.brewStatus) {
                BrewStatus.Manual -> Device.BrewStatus.Manual
                BrewStatus.Profile -> Device.BrewStatus.Profile
                BrewStatus.FreeVariable -> Device.BrewStatus.Profile
                else -> Device.BrewStatus.Idle
            }

            val currentButtonState = if (this.brewButtonState == BrewButtonState.Syncing) {
                BrewButtonState.Syncing
            } else if (newBrewStatus != Device.BrewStatus.Idle) {
                BrewButtonState.Brewing
            } else {
                BrewButtonState.Idle
            }

            copy(
                device = device.copy(
                    brewBoilerTemp = state.brewBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    steamBoilerTemp = state.steamBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    pressure = state.pressure?.roundDecimalsTo(1)?.toString(),
                    connectionStatus = when (state.connectionStatus) {
                        ConnectionStatus.Disconnected -> Device.ConnectionStatus.Disconnected
                        ConnectionStatus.Connecting -> Device.ConnectionStatus.Connecting
                        ConnectionStatus.Synchronizing -> Device.ConnectionStatus.Synchronizing
                        ConnectionStatus.Connected -> Device.ConnectionStatus.Connected
                    },
                    brewStatus = newBrewStatus,
                    smartScaleConnected = state.smartScale?.isConnected == true,
                    alarm = state.waterLevelAlarm,
                ),
                brewButtonState = currentButtonState,
            )
        }
    }

    /**
     * Keeps the machine's own target temperature — the long press is a power toggle, not a way to
     * change how hot the steam boiler runs.
     */
    private fun toggleSteamBoiler() {
        val config = deviceConfig ?: return
        val enabled = !config.steamBoilerEnabled
        launchCatching(::onError) {
            setBoilerSettings(
                deviceId = args.deviceId,
                boilerType = BoilerType.Steam,
                enabled = enabled,
                temp = config.targetSteamTemp.toInt(),
            )
            val message = if (enabled) {
                Res.string.device_dashboard_steam_boiler_on
            } else {
                Res.string.device_dashboard_steam_boiler_off
            }
            emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(getString(message)))
        }
    }

    private fun brewSessionDataChanged(session: BrewSession) {
        val wasBrewing = brewInProgress
        brewInProgress = session.inProgress

        if (session.mode == BrewMode.Manual && skipManualBrews) return

        if (!wasBrewing && session.inProgress) {
            modify {
                copy(
                    brew = Brew(
                        name = selectedProfileName.orEmpty(),
                        description = selectedProfileDescription.orEmpty(),
                    ),
                )
            }
        }
        modify {
            copy(
                brew = brew.copy(
                    time = session.elapsedSeconds,
                    data = session.toChartData(),
                    historyTarget = session.executionTrace?.toTargetData() ?: brew.historyTarget,
                    phaseTransitions = session.executionTrace?.transitions.orEmpty(),
                ),
            )
        }
        if (wasBrewing && !session.inProgress) recordBrew(session)
    }

    /** Only profile brews carry a profile — manual and freehand shots are labelled from their mode. */
    private fun recordBrew(session: BrewSession) = launchCatching(::onError) {
        val isProfileBrew = session.mode == BrewMode.Profile
        saveBrewToHistory(
            session = session,
            profileId = selectedProfileId?.toLongOrNull()?.takeIf { isProfileBrew },
            profileName = selectedProfileName?.takeIf { isProfileBrew },
            profileSteps = if (isProfileBrew) selectedProfileSteps else emptyList(),
            profileRecording = selectedProfileRecording.takeIf { isProfileBrew },
        )
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(visibleCharts = charts.toDashboard())
    }

    private fun withBluetoothPermissions(block: suspend () -> Unit) = launch {
        try {
            if (machine?.isDemo == false) {
                permissionsController.providePermission(PermissionBluetoothScan)
                permissionsController.providePermission(PermissionBluetoothConnect)
            }
            block()
        } catch (_: DeniedException) {
            showBluetoothPermissionMissingDialog()
        }
    }

    private fun withDeviceConnected(block: () -> Unit) {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            block()
        } else {
            onError(DeviceNotConnectedException(args.deviceId))
        }
    }

    private fun showBluetoothPermissionMissingDialog() {
        modify { copy(dialog = Dialog.BluetoothPermissionMissing) }
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch { emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(throwable.toUserMessage())) }
    }

    private fun DashboardChartType.toDomain() = when (this) {
        DashboardChartType.Pressure -> ChartType.PRESSURE
        DashboardChartType.FlowRate -> ChartType.FLOW_RATE
        DashboardChartType.WeightRate -> ChartType.WEIGHT_RATE
        DashboardChartType.Volume -> ChartType.VOLUME
        DashboardChartType.Weight -> ChartType.WEIGHT
    }
}

private fun Float.roundDecimalsTo(decimals: Int): Float {
    val factor = 10f.pow(decimals)
    return round(this * factor) / factor
}
