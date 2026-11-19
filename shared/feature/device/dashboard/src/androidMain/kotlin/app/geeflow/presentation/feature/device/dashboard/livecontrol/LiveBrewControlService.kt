package app.geeflow.presentation.feature.device.dashboard.livecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import app.geeflow.data.device.BookooScaleConnection
import app.geeflow.domain.device.LiveBrewTestCoordinator
import app.geeflow.domain.device.LiveControlSettings
import app.geeflow.domain.device.LiveModeProbePath
import app.geeflow.domain.device.LiveModeProbeSequence
import app.geeflow.domain.device.LiveTestRecipe
import app.geeflow.domain.device.recipe.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LiveBrewControlService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var deviceId = -1L
    private var controlJob: Job? = null
    private var observerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var cleaned = false
    private var scaleConnectedByService = false
    private var activeProbePath = LiveModeProbePath.EncodingMatrix

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getLongExtra(EXTRA_DEVICE_ID, deviceId)?.let { if (it > 0) deviceId = it }
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch {
                if (coordinator().owns(deviceId)) coordinator().stop(deviceId) else controlJob?.cancel()
            }
            return START_NOT_STICKY
        }
        val scaleId = intent?.getStringExtra(EXTRA_SCALE_ID)
        val commandProbe = intent?.getBooleanExtra(EXTRA_COMMAND_PROBE, false) == true
        val commandProbePath = LiveModeProbePath.entries.firstOrNull {
            it.id == intent?.getStringExtra(EXTRA_COMMAND_PROBE_PATH)
        } ?: LiveModeProbePath.EncodingMatrix
        val commandProbeSequence = LiveModeProbeSequence.entries.firstOrNull {
            it.id == intent?.getStringExtra(EXTRA_COMMAND_PROBE_SEQUENCE)
        } ?: LiveModeProbeSequence.FlowPressureFlowPressure
        activeProbePath = commandProbePath
        if (deviceId <= 0 || !commandProbe && scaleId.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (controlJob?.isActive == true) return START_NOT_STICKY
        scaleConnectedByService = false
        val recipeDocument = intent.getStringExtra(EXTRA_RECIPE_FILE)?.let { name ->
            require(name.matches(Regex("live-[a-zA-Z0-9-]+\\.json"))) { "Invalid execution snapshot name" }
            val snapshot = java.io.File(java.io.File(filesDir, "live-recipe-executions"), name)
            try { LiveRecipeJson.decode(snapshot.readText()) } finally { snapshot.delete() }
        }
        val settings = recipeDocument?.controlSettings() ?: settingsFrom(intent)
        createNotificationChannel()
        val initialNotification = notification(
            if (commandProbe) "Starting ${commandProbePath.label}: ${commandProbeSequence.label}"
            else "Checking scale before machine start",
        )
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
        acquireBoundedWakeLock(if (commandProbe) 100_000L else settings.shotDeadlineMillis)
        observeCoordinator()
        controlJob?.cancel()
        controlJob = serviceScope.launch {
            val previousRunId = coordinator().state.value.runId
            try {
                if (commandProbe) {
                    coordinator().startCommandEncodingProbe(deviceId, commandProbePath, commandProbeSequence, serviceScope)
                } else {
                    val scale = scaleConnection()
                    val selectedScaleId = requireNotNull(scaleId)
                    if (!scale.state.value.connected || scale.state.value.device?.id != selectedScaleId) {
                        scale.connect(selectedScaleId, serviceScope)
                        scaleConnectedByService = true
                    }
                    withTimeout(5.seconds) {
                        scale.state.first {
                            it.connected && it.device?.id == selectedScaleId &&
                                it.reading?.monotonicMark?.elapsedNow()?.inWholeMilliseconds?.let { age -> age in 0..1000 } == true
                        }
                    }
                    coordinator().startLive(
                        deviceId,
                        recipeDocument?.let(LiveTestRecipe::fromDocument) ?: LiveTestRecipe.simulationDefaults(settings.basePressureBar),
                        settings,
                        serviceScope,
                    )
                }
                coordinator().state.first { it.deviceId == deviceId && !it.active && !it.stopPending }
            } catch (error: Exception) {
                updateNotification("Test ended: ${error.message ?: "control error"}")
                delay(2.seconds)
            } finally {
                val finalState = coordinator().state.value
                if (finalState.runId != null && finalState.runId != previousRunId && finalState.captureEvents.isNotEmpty()) {
                    runCatching { exportLiveTestCapture(this@LiveBrewControlService, finalState) }
                        .onSuccess(coordinator()::reportExportLocation)
                }
                finishService()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeCoordinator() {
        if (observerJob?.isActive == true) return
        observerJob = serviceScope.launch {
            coordinator().state.collect { state ->
                if (state.deviceId == deviceId) {
                    val status = when {
                        state.stopPending -> "Stop requested, waiting for machine confirmation"
                        state.active && state.mode == app.geeflow.domain.device.LiveTestMode.CommandEncodingProbe -> "${activeProbePath.label} running. Tap Stop to end the test."
                        state.active -> "Live pressure control running. Tap Stop to end the shot."
                        else -> state.message ?: "Preparing live test"
                    }
                    updateNotification(status)
                }
            }
        }
    }

    private fun notification(message: String): Notification {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent().setClassName(packageName, "app.geeflow.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2,
            Intent(this, LiveBrewControlService::class.java)
                .setAction(ACTION_STOP).putExtra(EXTRA_DEVICE_ID, deviceId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("GeeFlow live brew test")
            .setContentText(message.take(120))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stop).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Live brew control", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(message))
    }

    private fun acquireBoundedWakeLock(deadlineMillis: Long) {
        val power = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GeeFlow:LiveBrewControl").apply {
            setReferenceCounted(false)
            acquire(deadlineMillis + WAKELOCK_GRACE_MILLIS)
        }
    }

    private suspend fun finishService() {
        if (cleaned) return
        cleaned = true
        if (scaleConnectedByService) runCatching { scaleConnection().disconnect() }
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    private fun settingsFrom(intent: Intent) = LiveControlSettings(
        basePressureBar = intent.getFloatExtra(EXTRA_BASE_PRESSURE, 3f),
        maximumPressureBar = intent.getFloatExtra(EXTRA_PRESSURE_CEILING, 9f),
        slewBarPerSecond = intent.getFloatExtra(EXTRA_SLEW, 1f),
        proportionalGainBarPerGramPerSecond = intent.getFloatExtra(EXTRA_KP, .5f),
        integralGainBarPerGram = intent.getFloatExtra(EXTRA_KI, .1f),
        commandIntervalMillis = intent.getLongExtra(EXTRA_COMMAND_INTERVAL, LiveControlSettings.DEFAULT_COMMAND_INTERVAL_MILLIS),
        shotDeadlineMillis = intent.getLongExtra(EXTRA_DEADLINE, 40_000L),
    )

    private fun coordinator() = GlobalContext.get().get<LiveBrewTestCoordinator>()
    private fun scaleConnection() = GlobalContext.get().get<BookooScaleConnection>()

    companion object {
        const val EXTRA_DEVICE_ID = "live_test.device_id"
        const val EXTRA_SCALE_ID = "live_test.scale_id"
        const val EXTRA_BASE_PRESSURE = "live_test.base_pressure"
        const val EXTRA_PRESSURE_CEILING = "live_test.pressure_ceiling"
        const val EXTRA_SLEW = "live_test.slew"
        const val EXTRA_KP = "live_test.kp"
        const val EXTRA_KI = "live_test.ki"
        const val EXTRA_COMMAND_INTERVAL = "live_test.command_interval"
        const val EXTRA_DEADLINE = "live_test.deadline"
        private const val ACTION_STOP = "app.geeflow.action.STOP_LIVE_BREW_TEST"
        private const val CHANNEL_ID = "live_brew_control"
        private const val NOTIFICATION_ID = 4702
        private const val WAKELOCK_GRACE_MILLIS = 6_000L
        const val EXTRA_RECIPE_FILE = "live_test.recipe_snapshot_file"
        const val EXTRA_COMMAND_PROBE = "live_test.command_encoding_probe"
        const val EXTRA_COMMAND_PROBE_PATH = "live_test.command_encoding_probe_path"
        const val EXTRA_COMMAND_PROBE_SEQUENCE = "live_test.command_encoding_probe_sequence"
    }
}
