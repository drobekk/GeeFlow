package app.geeflow.domain.device

import app.geeflow.data.device.AutoFlushSettingsRepository
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.AutoFlushSettings
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class AutoFlushCoordinator(
    private val settingsRepository: AutoFlushSettingsRepository,
    private val provider: DeviceControllerProvider,
    private val execution: ProfileExecutionCoordinator,
    private val scope: CoroutineScope,
    private val users: UserRepository,
    private val userSettings: UserSettingsRepository,
) {
    private val mutex = Mutex()
    private val foreground = MutableStateFlow(false)
    private val mutableCountdown = MutableStateFlow<AutoFlushCountdown?>(null)
    private val errorChannel = Channel<Throwable>(Channel.BUFFERED)
    private var countdownJob: Job? = null

    val countdown = mutableCountdown.asStateFlow()
    val errors = errorChannel.receiveAsFlow()

    init {
        scope.launch {
            provider.currentDeviceId.collectLatest { id ->
                mutex.withLock { cancelCountdown() }
                if (id != null) observeMachine(id)
            }
        }
    }

    fun setForeground(value: Boolean) {
        foreground.value = value
        if (!value) cancel()
    }

    fun cancel() {
        scope.launch { mutex.withLock { cancelCountdown() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeMachine(id: Long) {
        val controller = provider.getController(id)
        val trigger = AutoFlushTrigger()
        var settings = AutoFlushSettings()
        var manualBrew = false
        val treatManualAsFlush = users.selectedUser.flatMapLatest { user ->
            user?.let { userSettings.skipManualBrewHistory(it.id) } ?: flowOf(true)
        }
        combine(
            controller.deviceState,
            settingsRepository.observe(id),
            execution.state,
            treatManualAsFlush,
        ) { state, saved, run, manualAsFlush ->
            val status = if (run.active && run.deviceId == id) BrewStatus.Profile else state.brewStatus
            AutoFlushMachineState(
                state = state.copy(brewStatus = status),
                settings = saved,
                stopping = run.stopPending && run.deviceId == id,
                treatManualAsFlush = manualAsFlush,
            )
        }.collect { observation ->
            val state = observation.state
            mutex.withLock {
                if (observation.settings != settings) cancelCountdown()
                settings = observation.settings
                if (state.brewStatus != BrewStatus.Idle) manualBrew = state.brewStatus == BrewStatus.Manual
                val finished = trigger.onState(
                    status = state.brewStatus,
                    connected = state.connectionStatus == ConnectionStatus.Connected,
                    treatManualAsFlush = observation.treatManualAsFlush,
                )
                val skipManualFlush = manualBrew && observation.treatManualAsFlush
                val unavailable = observation.stopping || state.brewStatus != BrewStatus.Idle || skipManualFlush

                if (!canStart(id, controller, settings) || unavailable) {
                    cancelCountdown()
                } else if (finished) {
                    startCountdown(id, controller, settings, trigger)
                }
            }
        }
    }

    private fun startCountdown(
        id: Long,
        controller: DeviceController,
        settings: AutoFlushSettings,
        trigger: AutoFlushTrigger,
    ) {
        cancelCountdown()
        countdownJob = scope.launch {
            for (remaining in settings.delaySeconds downTo 1) {
                val ready = mutex.withLock {
                    if (canStart(id, controller, settings)) {
                        mutableCountdown.value = AutoFlushCountdown(id, remaining, settings.delaySeconds)
                        true
                    } else {
                        cancelCountdown()
                        false
                    }
                }
                if (!ready) return@launch
                delay(1.seconds)
            }
            mutex.withLock {
                mutableCountdown.value = null
                countdownJob = null
                startFlush(id, controller, settings, trigger)
            }
        }
    }

    private suspend fun startFlush(
        id: Long,
        controller: DeviceController,
        settings: AutoFlushSettings,
        trigger: AutoFlushTrigger,
    ) {
        try {
            execution.withUnownedControl(id) {
                if (canStart(id, controller, settings)) {
                    trigger.automaticStartRequested()
                    controller.startManualBrewing()
                }
            }
        } catch (error: CancellationException) {
            trigger.automaticStartFailed()
            throw error
        } catch (error: Exception) {
            trigger.automaticStartFailed()
            Logger.e(error) { "Failed to start auto flush" }
            errorChannel.trySend(error)
        }
    }

    private fun canStart(id: Long, controller: DeviceController, settings: AutoFlushSettings): Boolean {
        val state = controller.deviceState.value
        return provider.currentDeviceId.value == id && foreground.value && settings.enabled &&
            state.connectionStatus == ConnectionStatus.Connected && state.brewStatus == BrewStatus.Idle &&
            !state.waterLevelAlarm && !execution.owns(id) && DeviceCapability.ManualBrewing in controller.capabilities
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        mutableCountdown.value = null
    }
}
