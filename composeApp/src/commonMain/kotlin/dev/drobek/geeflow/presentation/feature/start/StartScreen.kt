package dev.drobek.geeflow.presentation.feature.start

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.drobek.geeflow.ui.GeeFlowTheme

@Composable
fun StartScreen(onClick: () -> Unit) {
    StartScreenContent(onClick)
}

@Composable
private fun StartScreenContent(onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.safeContentPadding().fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = onClick) {
            Text("Click e!")
        }
    }
}

@Composable
@Preview
private fun PreviewLight() = GeeFlowTheme(false) {
    StartScreenContent()
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowTheme(true) {
    StartScreenContent()
}
