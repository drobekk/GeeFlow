package app.geeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import app.geeflow.ui.theme.colorscheme.espressoColorScheme

@Preview(
    name = "Compact",
    fontScale = 1f,
)
@Preview(
    name = "Compact Dark",
    fontScale = 1f,
    uiMode = 0x20, // Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Expanded",
    widthDp = 840,
    heightDp = 460,
    fontScale = 1f,
)
@Preview(
    name = "Expanded Dark",
    widthDp = 840,
    heightDp = 460,
    fontScale = 1f,
    uiMode = 0x20, // Configuration.UI_MODE_NIGHT_YES
)
annotation class GeeFlowScreenPreview

@Preview(
    name = "Compact",
    fontScale = 1f,
)
@Preview(
    name = "Compact Dark",
    fontScale = 1f,
    uiMode = 0x20, // Configuration.UI_MODE_NIGHT_YES
)
annotation class GeeFlowComponentPreview

class GeeFlowPreviewWrapper : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        val isSystemInDarkTheme = isSystemInDarkTheme()
        GeeFlowTheme(
            darkMode = isSystemInDarkTheme,
            colorScheme = espressoColorScheme(isSystemInDarkTheme),
            content = content,
        )
    }
}

val isPreview: Boolean
    @Composable
    get() {
        return LocalInspectionMode.current
    }
