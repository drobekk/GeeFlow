package app.geeflow.ble

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import app.geeflow.data.device.ble.BleScanCoordinator
import app.geeflow.data.device.impl.controller.ConnectionDiagnosticSink
import app.geeflow.data.device.impl.controller.GeeFlowBleDiagnosticProbe
import app.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.Logger
import dev.bluefalcon.engine.android.AndroidEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Debug-only diagnostic activity. It is launched explicitly and does not enter normal app navigation. */
class ConnectionProtocolDiagnosticActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var sink: JsonlDiagnosticSink
    private lateinit var serialInput: EditText
    private lateinit var runButton: Button
    private lateinit var cancelButton: Button
    private lateinit var exportButton: Button
    private lateinit var statusText: TextView
    private lateinit var outputText: TextView
    private var runJob: Job? = null
    private var pendingSerial: String? = null

    private val exportLogLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-ndjson")) { destination ->
        if (destination == null) {
            if (::sink.isInitialized) sink.event("log_export_cancelled", emptyMap())
            return@registerForActivityResult
        }
        runCatching {
            val output = contentResolver.openOutputStream(destination) ?: error("Could not open the selected export destination")
            sink.file.inputStream().use { input -> output.use { input.copyTo(it) } }
        }.onSuccess {
            sink.event("log_exported", mapOf("destinationUri" to destination.toString(), "fileName" to sink.file.name))
            setStatus("Diagnostic log exported.")
        }.onFailure { failure ->
            sink.event("log_export_failed", mapOf("destinationUri" to destination.toString(), "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            setStatus("Could not export the diagnostic log: ${failure.message ?: failure::class.simpleName}")
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val requested = requiredPermissions()
        val denied = requested.filterNot { grants[it] == true || checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (denied.isEmpty()) {
            pendingSerial?.let(::startDiagnostic)
            pendingSerial = null
        } else {
            sink.event("permission_denied", mapOf("permissions" to denied.joinToString(",")))
            setStatus("Bluetooth permissions were denied. No scan started.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sink = JsonlDiagnosticSink(File(filesDir, "ble-diagnostics")) { line ->
            runOnUiThread {
                if (!::outputText.isInitialized) return@runOnUiThread
                outputText.text = (outputText.text.toString() + line + "\n").takeLast(MAX_VISIBLE_LOG_CHARS)
            }
        }
        renderDiagnosticScreen()
        sink.event(
            "diagnostic_screen_opened",
            mapOf(
                "fallbackSerial" to ConnectionDiagnosticProtocol.FALLBACK_SERIAL,
                "fallbackName" to ConnectionDiagnosticProtocol.FALLBACK_NAME,
                "fallbackMac" to ConnectionDiagnosticProtocol.FALLBACK_MAC,
                "logFile" to sink.file.absolutePath,
                "debugOnly" to "true",
            ),
        )
    }

    override fun onDestroy() {
        runJob?.cancel()
        activityScope.cancel()
        super.onDestroy()
    }

    private fun renderDiagnosticScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        root.addView(TextView(this).apply {
            text = "BLE connection protocol test"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Read-only diagnostic. For each method, the serial-name/data attempt and then the saved HCI name/MAC attempt each get up to 30 seconds. After the three comparisons, GeeFlow runs its extra read-only initialization checks. The unresolved FF55 language write is logged but not sent."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        })
        serialInput = EditText(this).apply {
            hint = "Type the espresso machine serial number"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine(true)
        }
        root.addView(serialInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        runButton = Button(this).apply {
            text = "Run all three methods"
            setOnClickListener { requestPermissionsAndRun() }
        }
        cancelButton = Button(this).apply {
            text = "Cancel"
            isEnabled = false
            setOnClickListener {
                runJob?.cancel()
                sink.event("run_cancelled_by_user", emptyMap())
                setStatus("Cancelling and closing the active BLE connection…")
            }
        }
        controls.addView(runButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(cancelButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(controls)

        exportButton = Button(this).apply {
            text = "Export diagnostic log"
            setOnClickListener { exportLogLauncher.launch(sink.file.name) }
        }
        root.addView(exportButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        statusText = TextView(this).apply {
            text = "Ready. Bluetooth must be enabled on the phone."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(statusText)
        root.addView(TextView(this).apply {
            text = "Saved log: ${sink.file.absolutePath}"
            textSize = 11f
            setPadding(0, dp(4), 0, dp(8))
        })
        outputText = TextView(this).apply {
            textSize = 11f
            gravity = Gravity.START
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(outputText)
        }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun requestPermissionsAndRun() {
        val serial = serialInput.text.toString().trim()
        if (serial.isEmpty()) {
            setStatus("Enter the machine serial number first.")
            return
        }
        pendingSerial = serial
        val missing = requiredPermissions().filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            sink.event("permission_request", mapOf("permissions" to missing.joinToString(",")))
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startDiagnostic(serial)
        }
    }

    private fun startDiagnostic(serial: String) {
        if (runJob?.isActive == true) return
        val manager = getSystemService(BluetoothManager::class.java)
        if (manager?.adapter?.isEnabled != true) {
            sink.event("bluetooth_unavailable", mapOf("reason" to "adapter_off_or_missing"))
            setStatus("Turn on Bluetooth on the Android phone, then run the diagnostic again.")
            return
        }
        runButton.isEnabled = false
        cancelButton.isEnabled = true
        outputText.text = ""
        setStatus("Starting GeeFlow, LitaLite-style, then Crema-style tests…")
        runJob = activityScope.launch {
            try {
                sink.event(
                    "run_started",
                    mapOf(
                        "typedSerial" to serial,
                        "fallbackSerialLabel" to ConnectionDiagnosticProtocol.FALLBACK_SERIAL,
                        "fallbackName" to ConnectionDiagnosticProtocol.FALLBACK_NAME,
                        "fallbackMac" to ConnectionDiagnosticProtocol.FALLBACK_MAC,
                        "targetAttemptBudgetMs" to "30000",
                        "maxComparisonBudgetMs" to "180000",
                        "maxGeeFlowReadOnlyInitializationBudgetMs" to GeeFlowBleDiagnosticProbe.INITIALIZATION_STAGE_BUDGET_MS.toString(),
                    ),
                )
                val blueFalcon = createBlueFalcon()
                val baseline = GeeFlowBleDiagnosticProbe(
                    blueFalcon = blueFalcon,
                    discoverer = WendougeeBleDeviceDiscoverer(),
                    scanCoordinator = BleScanCoordinator(blueFalcon, CoroutineScope(SupervisorJob() + Dispatchers.IO)),
                    sink = sink,
                )
                val baselineResult = withContext(Dispatchers.IO) {
                    baseline.run(serial, ConnectionDiagnosticProtocol.FALLBACK_MAC)
                }
                setStatus("GeeFlow method finished: ${if (baselineResult) "valid read" else "no valid read"}. Waiting before next method…")
                delay(BETWEEN_METHODS_MS)

                val comparison = AndroidConnectionComparisonProbe(this@ConnectionProtocolDiagnosticActivity, sink)
                val litaLiteResult = withContext(Dispatchers.IO) { comparison.run(ComparisonMethod.LITALITE, serial) }
                setStatus("LitaLite-style method finished: ${if (litaLiteResult) "valid read" else "no valid read"}. Waiting before next method…")
                delay(BETWEEN_METHODS_MS)

                val cremaResult = withContext(Dispatchers.IO) { comparison.run(ComparisonMethod.CREMA, serial) }
                setStatus("Comparisons finished. Running GeeFlow's separate read-only initialization checks…")
                val initializationResult = withContext(Dispatchers.IO) {
                    baseline.runReadOnlyInitializationStage(ConnectionDiagnosticProtocol.FALLBACK_MAC)
                }
                sink.event(
                    "run_finished",
                    mapOf(
                        "geeflowValidRead" to baselineResult.toString(),
                        "litaliteValidRead" to litaLiteResult.toString(),
                        "cremaValidRead" to cremaResult.toString(),
                        "geeflowReadOnlyInitializationAllSucceeded" to initializationResult.toString(),
                    ),
                )
                setStatus("Finished. GeeFlow=${baselineResult}; LitaLite-style=${litaLiteResult}; Crema-style=${cremaResult}; GeeFlow read-only initialization=${initializationResult}.")
            } catch (failure: Throwable) {
                sink.event("run_failed", mapOf("error" to (failure.message ?: failure::class.simpleName.orEmpty())))
                setStatus("Run stopped: ${failure.message ?: failure::class.simpleName}")
            } finally {
                runButton.isEnabled = true
                cancelButton.isEnabled = false
            }
        }
    }

    private fun createBlueFalcon(): BlueFalcon = BlueFalcon {
        engine = AndroidEngine(
            context = applicationContext as Application,
            logger = DiagnosticBlueFalconLogger,
        )
    }

    private fun requiredPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setStatus(value: String) {
        if (::statusText.isInitialized) statusText.text = value
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION = "app.geeflow.action.BLE_PROTOCOL_DIAGNOSTIC"
        private const val BETWEEN_METHODS_MS = 750L
        private const val MAX_VISIBLE_LOG_CHARS = 20_000
    }
}

private object DiagnosticBlueFalconLogger : Logger {
    override fun error(message: String, cause: Throwable?) { Log.e("GeeFlowBleDiag", message, cause) }
    override fun warn(message: String, cause: Throwable?) { Log.w("GeeFlowBleDiag", message, cause) }
    override fun info(message: String, cause: Throwable?) { Log.i("GeeFlowBleDiag", message, cause) }
    override fun debug(message: String, cause: Throwable?) { Log.d("GeeFlowBleDiag", message, cause) }
}

private class JsonlDiagnosticSink(
    directory: File,
    private val onLine: (String) -> Unit,
) : ConnectionDiagnosticSink {
    private val startedAt = android.os.SystemClock.elapsedRealtime()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val file: File

    init {
        directory.mkdirs()
        val runId = SimpleDateFormat("yyyyMMdd'T'HHmmss_SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        file = File(directory, "connection_$runId.jsonl")
        event("log_created", mapOf("schemaVersion" to "1", "runId" to runId))
    }

    @Synchronized
    override fun event(name: String, fields: Map<String, String>) {
        val timestamp = timestampFormat.format(Date())
        val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
        val body = buildString {
            append("{\"timestampUtc\":\"").append(timestamp.escapeJson())
            append("\",\"elapsedMs\":").append(elapsed)
            append(",\"event\":\"").append(name.escapeJson()).append("\",\"fields\":{")
            fields.entries.forEachIndexed { index, entry ->
                if (index > 0) append(',')
                append('\"').append(entry.key.escapeJson()).append("\":\"").append(entry.value.escapeJson()).append('\"')
            }
            append("}}\n")
        }
        FileOutputStream(file, true).use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }
        }
        Log.i("GeeFlowBleDiag", body.trimEnd())
        onLine("$timestamp +${elapsed}ms $name ${fields.entries.joinToString(" ") { "${it.key}=${it.value}" }}")
    }

    private fun String.escapeJson(): String = buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
    }
}
