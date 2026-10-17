package app.geeflow.platform

import androidx.compose.ui.platform.UriHandler

actual suspend fun UriHandler.openExternalUri(uri: String) = openUri(uri)
