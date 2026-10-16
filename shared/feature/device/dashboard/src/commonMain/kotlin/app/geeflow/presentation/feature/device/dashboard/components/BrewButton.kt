package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.geeflow.ui.components.WaveDivider
import app.geeflow.ui.icons.FlowControl
import app.geeflow.ui.icons.Flush
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_machine_busy
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

data class BrewButtonAction(
    val contentDescription: String,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

@Composable
fun BrewButton(
    state: BrewButtonState,
    sideActions: List<BrewButtonAction>,
    onHeroButtonClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    heroContentDescription: String = stringResource(Res.string.common_start),
) {
    require(sideActions.size in 1..MaxSideActions)
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
            BrewButtonState.Idle, BrewButtonState.Busy -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )
    val secondColor by animateColorAsState(
        when (state) {
            BrewButtonState.Brewing -> MaterialTheme.colorScheme.error
            BrewButtonState.Syncing -> MaterialTheme.colorScheme.tertiary
            BrewButtonState.Idle, BrewButtonState.Busy -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
    )
    val transition = updateTransition(targetState = state, label = "BrewingTransition")
    val heroAction = BrewButtonAction(heroContentDescription, onHeroButtonClick) {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.Logo),
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }

    Box(
        modifier = modifier
            .height(ButtonHeight)
            .width(IntrinsicSize.Min),
        contentAlignment = Alignment.Center,
    ) {
        WaveDivider(
            color = secondColor,
            amplitude = 5.dp,
            modifier = Modifier
                .background(firstColor, MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.large)
                .fillMaxHeight(),
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
                .fillMaxHeight(),
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
            ActiveStateContent(
                text = stringResource(Res.string.common_machine_busy).uppercase(),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.visible(buttonState == BrewButtonState.Busy),
            )
            BrewContent(
                sideActions = sideActions,
                heroAction = heroAction,
                enabled = state == BrewButtonState.Idle,
                onActionClick = { action ->
                    dismissTooltip()
                    action.onClick()
                },
                onActionLongClick = { action, alignment ->
                    showTransientTooltip(action.contentDescription, alignment)
                },
                modifier = Modifier.visible(buttonState == BrewButtonState.Idle),
            )
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
    sideActions: List<BrewButtonAction>,
    heroAction: BrewButtonAction,
    enabled: Boolean,
    onActionClick: (BrewButtonAction) -> Unit,
    onActionLongClick: (BrewButtonAction, Alignment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sideActions.forEach { action ->
            BrewSideButton(
                enabled = enabled,
                onClick = { onActionClick(action) },
                modifier = Modifier.semantics { contentDescription = action.contentDescription },
                onLongClick = { onActionLongClick(action, Alignment.TopStart) },
            ) {
                action.icon()
            }
        }
        BrewSideButton(
            enabled = enabled,
            onClick = { onActionClick(heroAction) },
            onLongClick = { onActionLongClick(heroAction, Alignment.TopEnd) },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = heroAction.contentDescription },
        ) {
            heroAction.icon()
        }
    }
}

@Composable
private fun RowScope.BrewSideButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .weight(1f)
            .widthIn(min = SideButtonMinWidth)
            .combinedClickable(
                enabled = enabled,
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
    Busy,
    Brewing
}

private const val TransitionDurationMs = 300
private val TransientTooltipDuration = 2000L.milliseconds

private const val MaxSideActions = 3

private val SideButtonMinWidth = 64.dp
private val ButtonHeight = 50.dp
private val HeroIconSize = 32.dp
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
        sideActions = listOf(
            BrewButtonAction(stringResource(Res.string.common_flush), {}) {
                Icon(rememberVectorPainter(GeeFlowIcon.Flush), contentDescription = null)
            },
            BrewButtonAction(stringResource(DashboardRes.string.free_control_screen_title), {}) {
                Icon(rememberVectorPainter(GeeFlowIcon.FlowControl), contentDescription = null)
            },
        ),
        onHeroButtonClick = { state = BrewButtonState.Brewing },
        onStopClick = { state = BrewButtonState.Idle },
    )
}
