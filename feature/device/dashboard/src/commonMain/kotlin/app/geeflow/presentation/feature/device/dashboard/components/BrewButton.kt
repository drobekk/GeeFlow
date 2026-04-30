package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.components.GeeFlowLogoShape
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.WaveDivider
import app.geeflow.ui.icons.FlowControl
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.icons.Manual
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_stop
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrewButton(
    isBrewing: Boolean,
    onManualClick: () -> Unit,
    onFlowClick: () -> Unit,
    onManualFlowClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstColor by animateColorAsState(
        if (isBrewing) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )
    val secondColor by animateColorAsState(
        if (isBrewing) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
    )
    val transition = updateTransition(targetState = isBrewing, label = "BrewingTransition")
    val logoOffsetY by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 200)
            } else {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            }
        },
        label = "logoOffset",
    ) { brewing -> if (brewing) 100.dp else 0.dp }
    val logoAlpha by transition.animateFloat(label = "logoAlpha") { if (it) 0f else 1f }

    Box(
        modifier = modifier
            .height(72.dp)
            .width(IntrinsicSize.Min),
        contentAlignment = Alignment.Center,
    ) {
        WaveDivider(
            color = secondColor,
            amplitude = 5.dp,
            modifier = Modifier
                .background(firstColor, MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.large)
                .height(50.dp),
        )
        transition.AnimatedContent(
            transitionSpec = {
                fadeIn(tween(TransitionDurationMs)) togetherWith fadeOut(tween(TransitionDurationMs))
            },
            modifier = Modifier
                .clickable(enabled = isBrewing, onClick = onStopClick, role = Role.Button)
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            BrewingContent(modifier = Modifier.visible(it))
            BrewContent(
                onManualClick = onManualClick,
                onManualFlowClick = onManualFlowClick,
                modifier = Modifier.visible(!it),
            )
        }

        if (logoAlpha > LogoVisibleThreshold) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = logoOffsetY)
                    .graphicsLayer { alpha = logoAlpha },
            ) {
                FilledIconButton(
                    shape = GeeFlowLogoShape,
                    onClick = onFlowClick,
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        painter = rememberVectorPainter(GeeFlowIcon.Logo),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).padding(top = 5.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrewContent(
    onManualClick: () -> Unit,
    onManualFlowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth().padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        IconButton(
            onClick = onManualClick,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            Icon(
                painter = rememberVectorPainter(GeeFlowIcon.Manual),
                modifier = Modifier.size(20.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(1f)
        IconButton(
            onClick = onManualFlowClick,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            Icon(
                painter = rememberVectorPainter(GeeFlowIcon.FlowControl),
                modifier = Modifier.size(28.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrewingContent(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.common_stop).uppercase(),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 32.dp),
    )
}

private const val TransitionDurationMs = 300
private const val LogoVisibleThreshold = 0.01f

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    var isBrewing by remember { mutableStateOf(false) }
    BrewButton(
        isBrewing = isBrewing,
        onManualClick = {},
        onFlowClick = { isBrewing = true },
        onManualFlowClick = {},
        onStopClick = { isBrewing = false },
    )
}
