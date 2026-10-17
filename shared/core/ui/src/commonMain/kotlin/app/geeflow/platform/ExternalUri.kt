package app.geeflow.platform

import androidx.compose.ui.platform.UriHandler

/** Opens a URI using the platform handler without blocking the desktop UI thread. */
expect suspend fun UriHandler.openExternalUri(uri: String)
