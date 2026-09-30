package app.geeflow.app.autoflush

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.geeflow.domain.device.AutoFlushCountdown
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.auto_flush_countdown_title
import geeflow.shared.core.ui.generated.resources.auto_flush_tap_to_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AutoFlushDialog(countdown: AutoFlushCountdown, onCancel: () -> Unit) {
    val progress = remember(countdown.deviceId, countdown.totalSeconds) {
        Animatable(countdown.remainingSeconds.toFloat() / countdown.totalSeconds)
    }
    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = countdown.remainingSeconds * MillisecondsPerSecond,
                easing = LinearEasing,
            ),
        )
    }
    Dialog(onDismissRequest = onCancel) {
        Surface(
            onClick = onCancel,
            modifier = Modifier.size(DialogDiameter),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(ProgressDiameter),
                    strokeWidth = 6.dp,
                )
                Column(
                    modifier = Modifier.width(CountdownTextWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.auto_flush_countdown_title),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = countdown.remainingSeconds.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = FontFamily.Serif,
                    )
                    Text(
                        text = stringResource(Res.string.auto_flush_tap_to_cancel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val DialogDiameter = 240.dp
private val ProgressDiameter = 216.dp
private val CountdownTextWidth = 180.dp
private const val MillisecondsPerSecond = 1000

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun PreviewThreeDigits() {
    AutoFlushDialog(
        countdown = AutoFlushCountdown(deviceId = 1, remainingSeconds = 120, totalSeconds = 120),
        onCancel = {},
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun PreviewOneDigit() {
    AutoFlushDialog(
        countdown = AutoFlushCountdown(deviceId = 1, remainingSeconds = 1, totalSeconds = 120),
        onCancel = {},
    )
}
