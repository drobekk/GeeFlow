package app.geeflow.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformContentInsets(): WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
