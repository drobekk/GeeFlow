@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.device.BookooScalePacketEvent
import app.geeflow.data.device.ScaleReadingSource
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.isDemo
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.FlowChartRange
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import app.geeflow.domain.brew.usecase.SaveBrewToHistoryUseCase
import app.geeflow.domain.brew.usecase.SaveBrewShotMetadataUseCase
import app.geeflow.presentation.feature.device.dashboard.model.brewShotSummary
import app.geeflow.domain.device.usecase.ConnectDeviceUseCase
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.CupFlowEstimator
import app.geeflow.domain.device.toBrewScaleSample
import app.geeflow.domain.device.usecase.DisconnectDeviceUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceBrewingSettingsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceCapabilitiesUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceUseCase
import app.geeflow.domain.device.usecase.RefreshAccessoriesUseCase
import app.geeflow.domain.device.usecase.StartHotWaterUseCase
import app.geeflow.domain.device.usecase.StartManualBrewingUseCase
import app.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import app.geeflow.domain.device.usecase.StopBrewingUseCase
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.user.usecase.GetFlowChartRangeUseCase
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.GetPreferredShotDoseUseCase
import app.geeflow.domain.user.usecase.SetPreferredShotDoseUseCase
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.SetFlowChartRangeUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.navigation.destination.FreeControl
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
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.LiveBrewTestClicked
import app.geeflow.presentation.feature.device.dashboard.livecontrol.LiveBrewTestRoute
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.HistoryBrewSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ManualBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.OpenSystemSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.PermissionDialogResumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ProfileSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.SettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.TemperatureClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.Resumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.StopBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Dialog
import app.geeflow.presentation.feature.device.dashboard.model.displayDescription
import app.geeflow.presentation.feature.device.dashboard.model.toChartData
import app.geeflow.presentation.feature.device.dashboard.model.toDashboard
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import app.geeflow.data.device.model.Device as Machine

@KoinViewModel
internal class DeviceDashboardViewModel(
    @InjectedParam private val args: DeviceDashboard,
    @InjectedParam val permissionsController: PermissionsController,
    private val observeDevice: ObserveDeviceUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val refreshAccessories: RefreshAccessoriesUseCase,
    private val startHotWater: StartHotWaterUseCase,
    private val observeDeviceCapabilities: ObserveDeviceCapabilitiesUseCase,
    private val startManualBrewing: StartManualBrewingUseCase,
    private val startProfileBrewing: StartProfileBrewingUseCase,
    private val stopBrewing: StopBrewingUseCase,
    private val observeBrewData: ObserveBrewDataUseCase,
    private val observeDeviceProfile: ObserveDeviceProfileUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val getFlowChartRange: GetFlowChartRangeUseCase,
    private val setFlowChartRange: SetFlowChartRangeUseCase,
    private val getBrewProfileUseCase: GetBrewProfileUseCase,
    private val getSelectedUser: GetSelectedUserUseCase,
    private val getPreferredShotDose: GetPreferredShotDoseUseCase,
    private val setPreferredShotDose: SetPreferredShotDoseUseCase,
    private val scaleReadings: ScaleReadingSource,
    private val saveBrewToHistory: SaveBrewToHistoryUseCase,
    private val saveBrewShotMetadata: SaveBrewShotMetadataUseCase,
    private val observeDeviceBrewingSettings: ObserveDeviceBrewingSettingsUseCase,
    private val profileExecutionCoordinator: ProfileExecutionCoordinator,
) : BaseViewModel<DeviceDashboardViewState, DeviceDashboardViewModelEvent>(DeviceDashboardViewState()) {

    private var selectedProfileId: String? = null
    private var selectedProfileName: String? = null
    private var selectedProfileDescription: String? = null
    private var machine: Machine? = null
    private var machineBusy = false
    private var brewInProgress = false
    private var pendingAppProfile: BrewProfile? = null
    private data class PendingShotRecipe(val dose: Float, val ratio: Float, val target: Float, val armedAt: kotlin.time.Instant)
    private var pendingShotRecipe: PendingShotRecipe? = null
    private var activeBrewProfile: BrewProfile? = null
    private var skipManualBrews = true
    private var activeScaleBrewStart: kotlin.time.Instant? = null
    private var activeScaleCaptureUntil: kotlin.time.Instant? = null
    private val activeScaleSamples = mutableListOf<BrewScaleSample>()
    private var previousScaleSample: BrewScaleSample? = null
    private val scaleFlowSmoother = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
    private val scaleCaptureMutex = Mutex()
    private val recentScalePackets = mutableListOf<BookooScalePacketEvent>()
    private val appendedScalePacketKeys = mutableSetOf<String>()

    init {
        launch {
            profileExecutionCoordinator.state.collect { execution ->
                if (execution.deviceId == args.deviceId) {
                    val trace = execution.trace ?: return@collect
                    modify {
                        copy(
                            brew = brew.copy(
                                historyStopWeightGrams = trace.stopTargetGrams ?: brew.historyStopWeightGrams,
                                historyFinalWeightGrams = trace.finalScaleWeightGrams ?: brew.historyFinalWeightGrams,
                                scaleSamples = trace.scaleSamples,
                            ),
                        )
                    }
                }
            }
        }
        launch {
            getPreferredShotDose().collect { dose -> modify { copy(preferredShotDoseGrams = dose) } }
        }
        launch { scaleReadings.packets.collect(::captureBookooPacket) }
        launch {
            observeDevice(args.deviceId).collect {
                machine = it
                it?.let {
                    modify {
                        copy(
                            device = device.copy(
                                id = args.deviceId,
                                name = machine?.name ?: args.deviceId.toString(),
                                isDemo = machine?.isDemo == true,
                            ),
                        )
                    }
                }
            }
        }
        launch {
            observeDeviceState(args.deviceId).combine(observeDeviceCapabilities(args.deviceId)) { state, capabilities ->
                state to (DeviceCapability.HotWaterDispensing in capabilities)
            }.collect { (state, hotWaterSupported) -> updateMachineStateUi(state, hotWaterSupported) }
        }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
        launch { getFlowChartRange().collect(::flowChartRangeChanged) }
        launch { observeBrewData(args.deviceId).collect(::brewSessionDataChanged) }
        launch { getSelectedUser().collect { u -> modify { copy(user = user.copy(photoFileName = u?.photoUri)) } } }
        launch {
            observeDeviceBrewingSettings(args.deviceId).collect { settings ->
                skipManualBrews = settings.treatManualAsFlush
                modify { copy(manualCyclesExcludedFromCharts = settings.treatManualAsFlush) }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is DeviceDashboardEvent.FlowChartRangeSelected -> launch { setFlowChartRange(event.range) }
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> navigate(To(DeviceList))
        is TemperatureClicked -> withDeviceConnected { navigate(To(QuickSettings(args.deviceId))) }
        is SettingsClicked, is UserClicked -> navigate(To(DeviceSettings(args.deviceId)))
        is ConnectedDevicesClicked -> withDeviceConnected {
            navigate(To(DeviceSettings(args.deviceId, EntryPoint.Connectivity)))
        }

        is CleaningClicked -> withDeviceConnected { navigate(To(QuickMaintenance(args.deviceId))) }
        is LiveBrewTestClicked -> navigate(To(LiveBrewTestRoute(args.deviceId)))
        is DeviceDashboardEvent.MaintenanceReminderOpened -> withDeviceConnected {
            navigate(To(QuickMaintenance(args.deviceId, event.type)))
        }

        is BrewDescriptionClicked -> modify {
            copy(dialog = Dialog.BrewDescription(brew.name, brew.description))
        }

        is EditProfileClicked -> editSelectedProfile()
        is DialogDismissed -> modify { copy(dialog = null) }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is DeviceDashboardEvent.HotWaterClicked -> launchCatching(::onError) { startHotWater(args.deviceId) }
        is ManualBrewClicked -> launchCatching(::onError) { startManualBrewing(args.deviceId) }
        is StopBrewClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is FlowControlClicked -> navigate(To(FreeControl(args.deviceId)))
        is BrewClicked -> startProfile(event)
        is DeviceDashboardEvent.ShotDoseChanged -> launchCatching(::onError) {
            setPreferredShotDose(event.doseGrams)
            modify { copy(preferredShotDoseGrams = event.doseGrams) }
        }
        is DeviceDashboardEvent.GrindDraftChanged -> modify { copy(grindDraft = event.value) }
        is DeviceDashboardEvent.GrindSettingChanged -> {
            val value = event.value.trim().takeIf { it.isNotEmpty() }
            modify { copy(brew = brew.copy(grindSetting = value)) }
            viewState.value.brew.startedAt?.let { startedAt ->
                saveBrewShotMetadata.updateGrindSetting(args.deviceId, startedAt, value)
                if (viewState.value.brew.historyId == null) profileExecutionCoordinator.updateShotMetadata(
                    deviceId = args.deviceId,
                    startedAt = startedAt,
                    doseGrams = viewState.value.brew.doseGrams,
                    grindSetting = value,
                    extractionStartMillis = viewState.value.brew.extractionStartMillis,
                    extractionStartEstimated = viewState.value.brew.extractionStartEstimated,
                )
            }
        }
        is DeviceDashboardEvent.ExtractionPhaseStarted -> {
            modify { copy(brew = brew.copy(extractionStartMillis = event.elapsedMillis, extractionStartEstimated = false)) }
            persistShotMetadata(viewState.value.brew)
        }
        is DeviceDashboardEvent.LiveRecipeSelected -> modify {
            copy(
                brew = (if (brew.historyId != null) Brew() else brew).copy(
                    name = event.name,
                    description = event.description,
                    historyTarget = null,
                    historyStopWeightGrams = null,
                    phaseProgram = null,
                    isLiveRecipe = true,
                    extractionPhaseId = event.extractionPhaseId,
                ),
            )
        }
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is ProfileSelected -> onProfileSelected(event.id)
        is HistoryBrewSelected -> modify {
            copy(
                brew = Brew(
                    name = event.name,
                    description = event.description,
                    time = event.durationSeconds,
                    data = event.data,
                    scaleSamples = event.scaleSamples,
                    historyTarget = event.targetData,
                    historyStopWeightGrams = event.targetWeightGrams,
                    phaseProgram = event.phaseProgram,
                    phaseTransitions = event.phaseTransitions,
                    recordedPhaseIds = event.recordedPhaseIds,
                    historyId = event.id,
                    startedAt = event.startedAt,
                    endedAt = event.endedAt,
                    doseGrams = event.doseGrams,
                    recipeRatio = event.recipeRatio,
                    recipeTargetGrams = event.recipeTargetGrams,
                    grindSetting = event.grindSetting,
                    extractionStartMillis = event.extractionStartMillis,
                    extractionStartEstimated = event.extractionStartEstimated,
                    finalWeightGrams = event.finalWeightGrams,
                ),
            )
        }

        is Resumed -> connect()
        is AlarmClicked -> modify { copy(dialog = Dialog.Errors) }
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            launchCatching { disconnectDevice(args.deviceId) }
        } else {
            withBluetoothPermissions {
                launchCatching { connectDevice(args.deviceId) }
            }
        }
    }

    private fun connect() {
        withBluetoothPermissions {
            when (viewState.value.device.connectionStatus) {
                Device.ConnectionStatus.Disconnected -> launchCatching { connectDevice(args.deviceId) }
                Device.ConnectionStatus.Connected -> launchCatching(
                    onError = { Logger.w(it) { "Could not refresh accessories" } },
                ) { refreshAccessories(args.deviceId) }

                else -> Unit
            }
        }
    }

    private fun editSelectedProfile() {
        val profileId = selectedProfileId?.toLongOrNull() ?: return
        modify { copy(dialog = null) }
        navigate(To(ProfileEditor(args.deviceId, profileId)))
    }

    private fun startProfile(event: BrewClicked) = launchCatching(::onError) {
        val targetWeightGrams = event.targetWeightGrams
        val stopAtYield = event.stopAtYield
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { profile ->
                var started = false
                try {
                    modify { copy(brewButtonState = BrewButtonState.Syncing) }
                    val recipeWeightTarget =
                        (profile.finishCondition as? app.geeflow.data.brew.model.Condition.Weight)?.target
                    val cutoffWeightTarget = targetWeightGrams?.let { requestedTarget ->
                        recipeWeightTarget?.let { recipeTarget -> earlierWeightTarget(recipeTarget, requestedTarget) }
                            ?: requestedTarget
                    }
                    val machineScaleAvailable = viewState.value.device.smartScaleConnected
                    val shotProfile = when {
                        recipeWeightTarget != null && cutoffWeightTarget != null && stopAtYield ->
                            profile.copy(finishCondition = app.geeflow.data.brew.model.Condition.Weight(cutoffWeightTarget))
                        recipeWeightTarget != null && !machineScaleAvailable && !stopAtYield ->
                            profile.copy(finishCondition = null)
                        else -> profile
                    }
                    pendingAppProfile = shotProfile
                    pendingShotRecipe = listOf(event.recipeDoseGrams, event.recipeRatio, event.recipeTargetGrams)
                        .takeIf { values -> values.all { it != null && it.isFinite() && it > 0f } }
                        ?.let { PendingShotRecipe(it[0]!!, it[1]!!, it[2]!!, Clock.System.now()) }
                    val weightStopUnavailable = startProfileBrewing(
                        args.deviceId,
                        shotProfile,
                        stopAtWeightGrams = cutoffWeightTarget.takeIf { stopAtYield },
                    )
                    started = true
                    if (weightStopUnavailable) {
                        emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar("Scale not ready: this brew will not stop at the selected weight."))
                    }
                } finally {
                    if (!started) {
                        pendingAppProfile = null
                        pendingShotRecipe = null
                    }
                    modify {
                        copy(
                            brewButtonState = if (machineBusy) {
                                BrewButtonState.Busy
                            } else if (device.isBrewing || device.dispensingHotWater) {
                                BrewButtonState.Brewing
                            } else {
                                BrewButtonState.Idle
                            },
                        )
                    }
                }
            }
    }

    private fun onProfileSelected(id: String?) = launchCatching(::onError) {
        selectedProfileId = id
        val profileId = id?.toLongOrNull() ?: return@launchCatching
        val profile = getBrewProfileUseCase(profileId) ?: return@launchCatching
        selectProfile(profile)
    }

    private suspend fun selectProfile(profile: BrewProfile) {
        selectedProfileId = profile.id.toString()
        selectedProfileName = profile.name
        selectedProfileDescription = profile.displayDescription()
        val activeExecution = profileExecutionCoordinator.state.value.takeIf {
            it.deviceId == args.deviceId && it.active
        }?.trace
        modify {
            copy(
                brew = Brew(
                    name = profile.name,
                    description = selectedProfileDescription.orEmpty(),
                    historyStopWeightGrams = activeExecution?.stopTargetGrams,
                    historyFinalWeightGrams = activeExecution?.finalScaleWeightGrams,
                ),
            )
        }
    }

    private fun updateMachineStateUi(state: DeviceState, hotWaterSupported: Boolean) {
        machineBusy = state.brewStatus == BrewStatus.Cleaning ||
            (state.brewStatus == BrewStatus.WaterFlow && !hotWaterSupported)
        modify {
            val newBrewStatus = when (state.brewStatus) {
                BrewStatus.Manual -> Device.BrewStatus.Manual
                BrewStatus.Profile -> Device.BrewStatus.Profile
                BrewStatus.FreeVariable -> Device.BrewStatus.Profile
                else -> Device.BrewStatus.Idle
            }

            val currentButtonState = if (machineBusy) {
                BrewButtonState.Busy
            } else if (state.brewStatus == BrewStatus.WaterFlow) {
                BrewButtonState.Brewing
            } else if (this.brewButtonState == BrewButtonState.Syncing) {
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
                    dispensingHotWater = state.brewStatus == BrewStatus.WaterFlow,
                    smartScaleConnected = state.smartScale?.isConnected == true,
                    alarm = state.hasError,
                    error = state.error,
                ),
                flushDurationSeconds = state.config?.manualBrewTimeSec?.toInt() ?: 0,
                flushPressure = state.config?.manualBrewPressure?.roundDecimalsTo(1)?.toString().orEmpty(),
                hotWaterSupported = hotWaterSupported,
                brewButtonState = currentButtonState,
            )
        }
    }

    private suspend fun brewSessionDataChanged(session: BrewSession) {
        val wasBrewing = brewInProgress
        brewInProgress = session.inProgress

        if (session.mode == BrewMode.Manual) {
            modify { copy(flushElapsedSeconds = session.elapsedSeconds) }
            if (skipManualBrews) return
        }

        if (!wasBrewing && session.inProgress) {
            val startedAt = session.startTime ?: kotlin.time.Clock.System.now()
            val pendingRecipe = pendingShotRecipe?.takeIf {
                session.mode == BrewMode.Profile && startedAt >= it.armedAt - 2.seconds && startedAt <= it.armedAt + 60.seconds
            }
            pendingShotRecipe = null
            val pendingBrew = viewState.value.brew
            val pendingGrind = viewState.value.grindDraft.trim().takeIf { it.isNotEmpty() }
            val pendingName = pendingBrew.name.takeIf { pendingBrew.isLiveRecipe }
            val pendingDescription = pendingBrew.description.takeIf { pendingBrew.isLiveRecipe }
            val pendingExtractionPhaseId = pendingBrew.extractionPhaseId.takeIf { pendingBrew.isLiveRecipe }
            scaleCaptureMutex.withLock {
                activeScaleBrewStart = startedAt
                activeScaleCaptureUntil = null
                activeScaleSamples.clear()
                previousScaleSample = null
                scaleFlowSmoother.reset()
                appendedScalePacketKeys.clear()
                scaleReadings.packets.replayCache
                    .filter { it.receivedAt >= startedAt }
                    .sortedBy { it.receivedAt }
                    .forEach(::appendScalePacket)
                recentScalePackets.removeAll { it.receivedAt < startedAt }
            }
            selectBrewSessionProfile(session)
            val description = activeBrewProfile?.displayDescription().orEmpty()
            modify {
                copy(
                    brew = Brew(
                        name = pendingName ?: activeBrewProfile?.name.orEmpty(),
                        description = pendingDescription ?: description,
                        startedAt = startedAt,
                        doseGrams = pendingRecipe?.dose ?: viewState.value.preferredShotDoseGrams,
                        recipeRatio = pendingRecipe?.ratio,
                        recipeTargetGrams = pendingRecipe?.target,
                        grindSetting = pendingGrind,
                        phaseProgram = activeBrewProfile?.program,
                        isLiveRecipe = pendingBrew.isLiveRecipe,
                        extractionPhaseId = pendingExtractionPhaseId,
                    ),
                    grindDraft = "",
                )
            }
            persistShotMetadata(viewState.value.brew)
        }
        val scaleSamples = scaleCaptureMutex.withLock {
            if (session.inProgress || wasBrewing) activeScaleSamples.toList() else session.scaleSamples
        }
        val recordedData = session.toChartData()
        val phaseProgram = session.executionTrace?.profile?.program ?: activeBrewProfile?.program
            ?: viewState.value.brew.phaseProgram
        val phaseTransitions = session.executionTrace?.transitions.orEmpty()
        val currentBrew = viewState.value.brew
        val endSeconds = session.endedAt?.let { endedAt ->
            session.startTime?.let { startedAt -> (endedAt - startedAt).inWholeMilliseconds / 1000.0 }
        }
        val finalWeight = scaleSamples.lastOrNull { it.decoded }?.weightGrams
            ?: recordedData.maxByOrNull { it.key }?.value?.weight?.takeIf { it > 0f }
        val summary = brewShotSummary(
            measurements = recordedData,
            scaleSamples = scaleSamples,
            profileProgram = phaseProgram,
            phaseTransitions = phaseTransitions,
            recordedExtractionStartMillis = currentBrew.extractionStartMillis,
            extractionStartEstimated = currentBrew.extractionStartEstimated,
            recordedEndSeconds = endSeconds,
            finalWeightGrams = finalWeight,
            shotInProgress = session.inProgress,
        )
        val resolvedExtractionStartMillis = if (
            summary.preinfusionTimeSeconds != null &&
            (!session.inProgress || summary.preinfusionTimeSeconds <= session.elapsedSeconds.toDouble())
        ) {
            (summary.preinfusionTimeSeconds * 1000).toLong()
        } else currentBrew.extractionStartMillis
        val splitEstimated = resolvedExtractionStartMillis?.let {
            currentBrew.extractionStartMillis?.let { currentBrew.extractionStartEstimated == true }
                ?: summary.splitEstimated
        }
        modify {
            copy(
                brew = brew.copy(
                    time = session.elapsedSeconds,
                    data = recordedData,
                    scaleSamples = scaleSamples,
                    historyTarget = session.executionTrace?.let { trace ->
                        if (session.inProgress) trace.profile.program.toTargetData(trace.transitions) else trace.toTargetData()
                    } ?: brew.historyTarget,
                    historyStopWeightGrams = (
                        session.executionTrace?.profile?.finishCondition as? Condition.Weight
                    )?.target
                        ?: (activeBrewProfile?.finishCondition as? Condition.Weight)?.target
                        ?: brew.historyStopWeightGrams,
                    phaseProgram = phaseProgram,
                    phaseTransitions = phaseTransitions,
                    recordedPhaseIds = session.executionTrace?.targets.orEmpty().mapNotNull { it.phaseId }.toSet(),
                    startedAt = session.startTime ?: brew.startedAt,
                    endedAt = session.endedAt,
                    doseGrams = brew.doseGrams ?: if (session.inProgress) preferredShotDoseGrams else null,
                    extractionStartMillis = resolvedExtractionStartMillis,
                    extractionStartEstimated = splitEstimated,
                    finalWeightGrams = finalWeight,
                ),
            )
        }
        val updatedBrew = viewState.value.brew
        if (!session.inProgress || currentBrew.extractionStartMillis != updatedBrew.extractionStartMillis) {
            persistShotMetadata(updatedBrew)
        }
        if (wasBrewing && !session.inProgress) {
            val completedSamples = scaleCaptureMutex.withLock {
                activeScaleCaptureUntil = (viewState.value.brew.endedAt ?: Clock.System.now()) + 3.seconds
                activeScaleSamples.toList()
            }
            recordBrew(session.copy(scaleSamples = completedSamples))
        }
    }

    private suspend fun captureBookooPacket(packet: BookooScalePacketEvent) = scaleCaptureMutex.withLock {
        recentScalePackets += packet
        if (recentScalePackets.size > MAX_RECENT_SCALE_PACKETS) recentScalePackets.removeAt(0)
        val startedAt = activeScaleBrewStart ?: return@withLock
        if (packet.receivedAt < startedAt) return@withLock
        val captureUntil = activeScaleCaptureUntil
        if (captureUntil != null && packet.receivedAt > captureUntil) {
            activeScaleBrewStart = null
            activeScaleCaptureUntil = null
            return@withLock
        }
        appendScalePacket(packet)
    }

    private fun appendScalePacket(packet: BookooScalePacketEvent) {
        val startedAt = activeScaleBrewStart ?: return
        if (packet.receivedAt < startedAt) return
        val key = "${packet.receivedAt.toEpochMilliseconds()}:${packet.packetHex}"
        if (!appendedScalePacketKeys.add(key)) return
        val elapsed = (packet.receivedAt - startedAt).inWholeMilliseconds.coerceAtLeast(0L)
        val smoothedFlow = if (packet.decoded) packet.frame?.let { scaleFlowSmoother.add(elapsed, it.weightGrams) } else null
        val sample = packet.toBrewScaleSample(startedAt, previousScaleSample, smoothedFlow)
        if (sample.decoded) previousScaleSample = sample
        val sampleWeight = sample.weightGrams
        activeScaleSamples += sample
        val recordedSamples = activeScaleSamples.toList()
        val currentBrew = viewState.value.brew
        val postStop = currentBrew.startedAt == startedAt && currentBrew.endedAt != null
        if (currentBrew.startedAt == startedAt) {
            modify {
                copy(
                    brew = brew.copy(
                        scaleSamples = recordedSamples,
                        finalWeightGrams = if (postStop && sample.decoded) sampleWeight ?: brew.finalWeightGrams else brew.finalWeightGrams,
                    ),
                )
            }
        }
        if (postStop && sample.decoded && sampleWeight != null) {
            saveBrewShotMetadata.updateFinalWeight(args.deviceId, startedAt, sampleWeight)
        }
    }

    /**
     * Selects the started profile, using the bound profile for brews started on the machine.
     * Opens its details and remembers it for brew history.
     */
    private suspend fun selectBrewSessionProfile(session: BrewSession) {
        val appProfile = pendingAppProfile
        pendingAppProfile = null
        activeBrewProfile = session.executionTrace?.profile ?: appProfile
            ?: if (session.mode == BrewMode.Profile) observeDeviceProfile(args.deviceId).first() else null
        activeBrewProfile?.let { profile ->
            selectProfile(profile)
        }
    }

    /** Only profile brews carry a profile — manual and freehand shots are labelled from their mode. */
    private fun recordBrew(session: BrewSession) {
        val profile = activeBrewProfile
        launchCatching(::onError) {
            val brew = viewState.value.brew
            val summary = brewShotSummary(
                measurements = session.toChartData(),
                scaleSamples = session.scaleSamples,
                profileProgram = brew.phaseProgram ?: profile?.program,
                phaseTransitions = session.executionTrace?.transitions.orEmpty(),
                recordedExtractionStartMillis = brew.extractionStartMillis,
                extractionStartEstimated = brew.extractionStartEstimated,
                recordedEndSeconds = session.endedAt?.let { endedAt ->
                    session.startTime?.let { startedAt -> (endedAt - startedAt).inWholeMilliseconds / 1000.0 }
                },
                finalWeightGrams = brew.finalWeightGrams,
            )
            saveBrewToHistory(
                deviceId = args.deviceId,
                session = session,
                profileId = profile?.id,
                profileName = brew.name.takeIf { brew.isLiveRecipe } ?: profile?.name,
                profileSteps = profile?.steps.orEmpty(),
                profileRecording = profile?.recording,
                profileProgram = brew.phaseProgram ?: profile?.program,
                doseGrams = brew.doseGrams,
                grindSetting = brew.grindSetting,
                extractionStartMillis = summary.preinfusionTimeSeconds?.let { (it * 1000).toLong() },
                extractionStartEstimated = summary.preinfusionTimeSeconds?.let { summary.splitEstimated },
            )
        }
    }

    private fun persistShotMetadata(brew: DeviceDashboardViewState.Brew) {
        val startedAt = brew.startedAt ?: return
        saveBrewShotMetadata(
            deviceId = args.deviceId,
            startedAt = startedAt,
            doseGrams = brew.doseGrams,
            grindSetting = brew.grindSetting,
            endedAt = brew.endedAt,
            extractionStartMillis = brew.extractionStartMillis,
            extractionStartEstimated = brew.extractionStartEstimated,
            finalWeightGrams = brew.finalWeightGrams,
            recipeRatio = brew.recipeRatio,
            recipeTargetGrams = brew.recipeTargetGrams,
        )
        profileExecutionCoordinator.updateShotMetadata(
            deviceId = args.deviceId,
            startedAt = startedAt,
            doseGrams = brew.doseGrams,
            grindSetting = brew.grindSetting,
            extractionStartMillis = brew.extractionStartMillis,
            extractionStartEstimated = brew.extractionStartEstimated,
        )
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(visibleCharts = charts.toDashboard())
    }

    private fun flowChartRangeChanged(range: FlowChartRange) = modify {
        copy(flowChartRange = range)
    }

    internal fun withBluetoothPermissions(
        requireBluetoothPermission: Boolean = false,
        block: suspend () -> Unit,
    ) = launch {
        try {
            if (requireBluetoothPermission || machine?.isDemo == false) {
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

private const val MAX_RECENT_SCALE_PACKETS = 256

private fun Float.roundDecimalsTo(decimals: Int): Float {
    val factor = 10f.pow(decimals)
    return round(this * factor) / factor
}
