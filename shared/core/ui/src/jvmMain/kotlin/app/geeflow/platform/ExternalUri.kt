package app.geeflow.platform

import androidx.compose.ui.platform.UriHandler
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Windows Desktop.browse initializes COM as STA. Keep it off the AWT thread and shared
// worker pools, whose COM apartment may already have been initialized by another native library.
private val browserDispatcher by lazy {
    Executors.newSingleThreadExecutor { task ->
        Thread(task, "GeeFlow-browser").apply { isDaemon = true }
    }.asCoroutineDispatcher()
}

actual suspend fun UriHandler.openExternalUri(uri: String) {
    withContext(browserDispatcher) { openUri(uri) }
}
