package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberLiveRecipeFiles(onImported: (String) -> Unit, onMessage: (String) -> Unit): LiveRecipeFiles {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imported by rememberUpdatedState(onImported)
    val message by rememberUpdatedState(onMessage)
    var pendingExport by remember { mutableStateOf("") }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 2_000_000) { "Recipe file exceeds 2 MB" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray().toString(Charsets.UTF_8)
                    }
                }
            }.onSuccess(imported).onFailure { message(it.message ?: "Could not read recipe") }
        }
    }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val text = pendingExport
            runCatching { withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(text) } } }
                .onSuccess { message("Recipe exported") }.onFailure { message(it.message ?: "Could not export recipe") }
        }
    }
    return remember(open, create) { object : LiveRecipeFiles {
        override fun openImport() = open.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        override fun export(name: String, text: String) { pendingExport = text; create.launch(name) }
    } }
}
