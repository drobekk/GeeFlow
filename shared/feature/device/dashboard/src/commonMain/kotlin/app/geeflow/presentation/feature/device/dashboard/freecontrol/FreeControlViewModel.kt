@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.freecontrol

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.RecordingCapacityExceededException
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.user.model.ChartType
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.brew.usecase.SaveFreeVariableProfileUseCase
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.GetDeviceUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.SetFreeBrewFlowUseCase
import app.geeflow.domain.device.usecase.SetFreeBrewPressureUseCase
import app.geeflow.domain.device.usecase.StartFreeVariableBrewingUseCase
import app.geeflow.domain.device.usecase.StopFreeVariableBrewingUseCase
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.destination.FreeControl
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ModeChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameConfirmed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameDismissed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StartClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StopClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.TargetChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.model.toChartData
import app.geeflow.presentation.feature.device.dashboard.model.toDashboard
import app.geeflow.presentation.feature.device.dashboard.model.toDomain
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_profile_saved
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_recording_too_long
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
internal class FreeControlViewModel(
    @InjectedParam private val args: FreeControl,
    getDevice: GetDeviceUseCase,
    observeDeviceState: ObserveDeviceStateUseCase,
    observeBrewData: ObserveBrewDataUseCase,
    private val startFreeVariableBrewing: StartFreeVariableBrewingUseCase,
    private val stopFreeVariableBrewing: StopFreeVariableBrewingUseCase,
    private val setFreeBrewPressure: SetFreeBrewPressureUseCase,
    private val setFreeBrewFlow: SetFreeBrewFlowUseCase,
    private val saveFreeVariableProfile: SaveFreeVariableProfileUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val appScope: CoroutineScope,
) : BaseViewModel<FreeControlViewState, FreeControlViewModelEvent>(FreeControlViewState()) {

    private var lastSession: BrewSession? = null
    private var targetUpdateJob: Job? = null

    init {
        val device = getDevice(args.deviceId)
        modify { copy(deviceName = device?.name.orEmpty()) }
        launch {
            val constraints = getDeviceConstraints(args.deviceId)
            modify { copy(pressureRange = constraints.pressureRange, flowRange = constraints.flowRange) }
        }
        launch { observeDeviceState(args.deviceId).collect(::updateFromDeviceState) }
        launch {
            observeBrewData(args.deviceId).collect { session ->
                lastSession = session
                brewSessionDataChanged(session)
            }
        }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
    }

    fun handleEvent(event: FreeControlEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is ModeChanged -> onModeChanged(event)
        is TargetChanged -> onTargetChanged(event.value)
        is StartClicked -> startBrewing()
        is StopClicked -> {
            targetUpdateJob?.cancel()
            launchCatching(::onError) { stopFreeVariableBrewing(args.deviceId) }
        }
        is SaveClicked -> saveSession()
        is RenameClicked -> modify { copy(showRenameDialog = true) }
        is RenameConfirmed -> modify { copy(profileName = event.name, showRenameDialog = false) }
        is RenameDismissed -> modify { copy(showRenameDialog = false) }
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
    }

    private fun startBrewing(): Job = launchCatching(::onError) {
        targetUpdateJob?.cancel()
        modify { copy(sessionCompleted = false) }
        val isFlow = viewState.value.mode == ControlMode.Flow
        startFreeVariableBrewing(args.deviceId, isFlow)
        if (isFlow) {
            setFreeBrewFlow(args.deviceId, viewState.value.flowTarget)
        } else {
            setFreeBrewPressure(args.deviceId, viewState.value.pressureTarget)
        }
    }

    private fun onModeChanged(event: ModeChanged) {
        if (viewState.value.brewStatus == FreeBrewStatus.Idle) {
            modify { copy(mode = event.mode) }
        }
    }

    private fun onTargetChanged(value: Float) {
        if (viewState.value.mode == ControlMode.Pressure) {
            modify { copy(pressureTarget = value) }
        } else {
            modify { copy(flowTarget = value) }
        }
        targetUpdateJob?.cancel()
        targetUpdateJob = launch {
            delay(TARGET_UPDATE_DEBOUNCE_MS.milliseconds)
            launchCatching(::onError) {
                if (viewState.value.mode == ControlMode.Pressure) {
                    setFreeBrewPressure(args.deviceId, viewState.value.pressureTarget)
                } else {
                    setFreeBrewFlow(args.deviceId, viewState.value.flowTarget)
                }
            }
        }
    }

    private fun saveSession() {
        val session = lastSession ?: return
        val name = viewState.value.profileName.ifBlank { return }
        launchCatching(::onError) {
            saveFreeVariableProfile(session, name)
            emitEvent(ShowSnackbar(getString(Res.string.free_control_profile_saved)))
        }
    }

    private fun updateFromDeviceState(state: DeviceState) {
        val wasActive = viewState.value.brewStatus == FreeBrewStatus.Active
        val isNowIdle = state.brewStatus == DeviceState.BrewStatus.Idle
        modify {
            copy(
                brewStatus = if (state.brewStatus == DeviceState.BrewStatus.FreeVariable) {
                    FreeBrewStatus.Active
                } else {
                    FreeBrewStatus.Idle
                },
                sessionCompleted = sessionCompleted || (wasActive && isNowIdle),
            )
        }
    }

    private fun brewSessionDataChanged(session: BrewSession) = modify {
        copy(brew = brew.copy(time = session.elapsedSeconds, data = session.toChartData()))
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(visibleCharts = charts.toDashboard())
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch {
            val message = if (throwable is RecordingCapacityExceededException) {
                getString(Res.string.free_control_recording_too_long)
            } else {
                throwable.toUserMessage()
            }
            emitEvent(ShowSnackbar(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (viewState.value.brewStatus == FreeBrewStatus.Active) {
            appScope.launch {
                runCatching { stopFreeVariableBrewing(args.deviceId) }
                    .onFailure { Logger.e(throwable = it) { "${this::class.simpleName}" } }
            }
        }
    }

    companion object {
        private const val TARGET_UPDATE_DEBOUNCE_MS = 200L
    }
}
