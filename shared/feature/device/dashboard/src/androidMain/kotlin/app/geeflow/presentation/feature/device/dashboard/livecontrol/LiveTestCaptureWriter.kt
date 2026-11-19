package app.geeflow.presentation.feature.device.dashboard.livecontrol

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.geeflow.domain.device.LiveTestCaptureFormatter
import app.geeflow.domain.device.LiveTestRunState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun exportLiveTestCapture(context: Context, state: LiveTestRunState): String {
    val name = "live_brew_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
    val outputs = listOf(
        Triple("$name.jsonl", "application/x-ndjson", LiveTestCaptureFormatter.jsonl(state)),
        Triple("$name.csv", "text/csv", LiveTestCaptureFormatter.csv(state)),
        Triple("$name-report.txt", "text/plain", LiveTestCaptureFormatter.verificationReport(state)),
    )
    if (Build.VERSION.SDK_INT >= 29) {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/GeeFlowLiveTests"
        outputs.forEach { (fileName, mimeType, content) ->
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) }
                    ?: error("Could not open exported capture file")
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            } catch (error: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }
        return "Downloads/GeeFlowLiveTests/$name"
    }

    val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "live-test-captures")
        .apply { mkdirs() }
    outputs.forEach { (fileName, _, content) -> File(directory, fileName).writeText(content) }
    return directory.absolutePath
}
