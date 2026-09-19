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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.geeflow.ui.components.GeeFlowLogoShape
import app.geeflow.ui.components.WaveDivider
import app.geeflow.ui.icons.FlowControl
import app.geeflow.ui.icons.Flush
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_start
import geeflow.shared.core.ui.generated.resources.common_stop
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_syncing
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_screen_title
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import geeflow.shared.feature.device.dashboard.generated.resources.Res as DashboardRes

@Composable
fun BrewButton(
    state: BrewButtonState,
    onStartButtonClick: () -> Unit,
    onHeroButtonClick: () -> Unit,
    onEndButtonClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    startContentDescription: String = stringResource(Res.string.common_flush),
    heroContentDescription: String = stringResource(Res.string.common_start),
    endContentDescription: String = stringResource(DashboardRes.string.free_control_screen_title),
    startIcon: @Composable () -> Unit = {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.Flush),
            contentDescription = startContentDescription,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
    endIcon: @Composable () -> Unit = {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.FlowControl),
            contentDescription = endContentDescription,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var tooltipJob by remember { mutableStateOf<Job?>(null) }
    var activeTooltipText by remember { mutableStateOf<String?>(null) }
    var activeTooltipAlignment by remember { mutableStateOf(Alignment.TopCenter) }

    fun showTransientTooltip(text: String, alignment: Alignment) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        tooltipJob?.cancel()
        activeTooltipText = text
        activeTooltipAlignment = alignment
        tooltipJob = coroutineScope.launch {
            delay(TransientTooltipDuration)
            activeTooltipText = null
        }
    }

    fun dismissTooltip() {
        tooltipJob?.cancel()
        activeTooltipText = null
    }

    val firstColor by animateColorAsState(
        when (state) {
            BrewButtonState.Brewing -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            BrewButtonState.Syncing -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
            BrewButtonState.Idle -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )
    val secondColor by animateColorAsState(
        when (state) {
            BrewButtonState.Brewing -> MaterialTheme.colorScheme.error
            BrewButtonState.Syncing -> MaterialTheme.colorScheme.tertiary
            BrewButtonState.Idle -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
    )
    val transition = updateTransition(targetState = state, label = "BrewingTransition")
    val logoOffsetY by transition.animateDp(
        transitionSpec = {
            if (targetState != BrewButtonState.Idle) {
                tween(durationMillis = 200)
            } else {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            }
        },
        label = "logoOffset",
    ) { buttonState -> if (buttonState != BrewButtonState.Idle) 100.dp else 0.dp }
    val logoAlpha by transition.animateFloat(label = "logoAlpha") { if (it != BrewButtonState.Idle) 0f else 1f }

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
                .clickable(
                    enabled = state == BrewButtonState.Brewing,
                    onClick = {
                        dismissTooltip()
                        onStopClick()
                    },
                    role = Role.Button,
                )
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) { buttonState ->
            ActiveStateContent(
                text = stringResource(Res.string.common_stop).uppercase(),
                textColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.visible(buttonState == BrewButtonState.Brewing),
            )
            ActiveStateContent(
                text = stringResource(DashboardRes.string.device_dashboard_syncing).uppercase(),
                textColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.visible(buttonState == BrewButtonState.Syncing),
            )
            BrewContent(
                onStartButtonClick = {
                    dismissTooltip()
                    onStartButtonClick()
                },
                onEndButtonClick = {
                    dismissTooltip()
                    onEndButtonClick()
                },
                onStartButtonLongClick = {
                    showTransientTooltip(startContentDescription, Alignment.TopStart)
                },
                onEndButtonLongClick = {
                    showTransientTooltip(endContentDescription, Alignment.TopEnd)
                },
                startIcon = startIcon,
                endIcon = endIcon,
                modifier = Modifier.visible(buttonState == BrewButtonState.Idle),
            )
        }

        if (logoAlpha > LogoVisibleThreshold) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = logoOffsetY)
                    .graphicsLayer { alpha = logoAlpha }
                    .size(HeroButtonSize)
                    .background(MaterialTheme.colorScheme.primary, GeeFlowLogoShape)
                    .clip(GeeFlowLogoShape)
                    .combinedClickable(
                        onClick = {
                            dismissTooltip()
                            onHeroButtonClick()
                        },
                        onLongClick = {
                            showTransientTooltip(heroContentDescription, Alignment.TopCenter)
                        },
                        role = Role.Button,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = rememberVectorPainter(GeeFlowIcon.Logo),
                    contentDescription = heroContentDescription,
                    modifier = Modifier.size(HeroIconSize).padding(top = HeroIconTopPadding),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        activeTooltipText?.let { tooltipText ->
            val yOffsetPx = with(density) { TooltipVerticalOffset.roundToPx() }
            val xOffsetPx = when (activeTooltipAlignment) {
                Alignment.TopStart -> with(density) { TooltipHorizontalOffset.roundToPx() }
                Alignment.TopEnd -> with(density) { (-TooltipHorizontalOffset).roundToPx() }
                else -> 0
            }
            Popup(
                alignment = activeTooltipAlignment,
                offset = IntOffset(x = xOffsetPx, y = yOffsetPx),
                onDismissRequest = { activeTooltipText = null },
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = TooltipShadowElevation,
                ) {
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            horizontal = TooltipHorizontalPadding,
                            vertical = TooltipVerticalPadding,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrewContent(
    onStartButtonClick: () -> Unit,
    onEndButtonClick: () -> Unit,
    onStartButtonLongClick: () -> Unit,
    onEndButtonLongClick: () -> Unit,
    startIcon: @Composable () -> Unit,
    endIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrewSideButton(
            onClick = onStartButtonClick,
            onLongClick = onStartButtonLongClick,
        ) {
            startIcon()
        }
        Box(modifier = Modifier.width(HeroButtonSize))
        BrewSideButton(
            onClick = onEndButtonClick,
            onLongClick = onEndButtonLongClick,
        ) {
            endIcon()
        }
    }
}

@Composable
private fun RowScope.BrewSideButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .weight(1f)
            .widthIn(min = SideButtonMinWidth)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ActiveStateContent(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 32.dp),
    )
}

enum class BrewButtonState {
    Idle,
    Syncing,
    Brewing
}

private const val TransitionDurationMs = 300
private const val LogoVisibleThreshold = 0.01f
private val TransientTooltipDuration = 2000L.milliseconds

private val SideButtonMinWidth = 82.dp
private val HeroButtonSize = 72.dp
private val HeroIconSize = 48.dp
private val HeroIconTopPadding = 5.dp
private val TooltipVerticalOffset = (-44).dp
private val TooltipHorizontalOffset = 32.dp
private val TooltipShadowElevation = 4.dp
private val TooltipHorizontalPadding = 10.dp
private val TooltipVerticalPadding = 6.dp

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    var state by remember { mutableStateOf(BrewButtonState.Idle) }
    BrewButton(
        state = state,
        onStartButtonClick = {},
        onHeroButtonClick = { state = BrewButtonState.Brewing },
        onEndButtonClick = {},
        onStopClick = { state = BrewButtonState.Idle },
    )
}
