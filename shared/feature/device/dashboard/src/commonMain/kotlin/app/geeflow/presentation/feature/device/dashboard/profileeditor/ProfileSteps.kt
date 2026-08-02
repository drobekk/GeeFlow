package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.AddStepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepRemoved
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepsReordered
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.modifier.squareSize
import app.geeflow.ui.theme.disabled
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_add_step
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_end_step
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_remove_step
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val CardSpacing = 8.dp
private val StepTileWidth = 140.dp
private val EndToggleHeight = 60.dp
private val BadgeIconSize = 14.dp
private val BadgeIconOffset = 8.dp
private val BadgeIconPadding = 3.dp
private val DraggingElevation = 6.dp
private const val EndToggleIconFraction = 0.4f
private const val SelectedContainerAlpha = 0.16f

private const val AddStepKey = "add_step"
private const val EndStepKey = "end_step"

private enum class StepCardStyle {
    Row,
    Tile,
}

@Composable
internal fun ProfileStepsColumn(
    steps: List<Step>,
    finishTarget: FinishTarget,
    onEvent: (ProfileEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onEvent(StepsReordered(from.index, to.index))
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            ReorderableItem(state = reorderableState, key = step.id) { isDragging ->
                StepCard(
                    step = step,
                    number = index + 1,
                    style = StepCardStyle.Row,
                    isDragging = isDragging,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .longPressDraggableHandle(onDragStarted = { haptic.performHapticFeedback(LongPress) }),
                )
            }
        }
        item(key = AddStepKey) {
            AddStepButton(
                onClick = { onEvent(AddStepClicked) },
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
        item(key = EndStepKey) {
            EndStepCard(
                finishTarget = finishTarget,
                onTypeClick = { onEvent(FinishTargetClicked(it)) },
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
    }
}

@Composable
internal fun ProfileStepsRow(
    steps: List<Step>,
    finishTarget: FinishTarget,
    onEvent: (ProfileEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onEvent(StepsReordered(from.index, to.index))
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyRow(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            ReorderableItem(state = reorderableState, key = step.id) { isDragging ->
                StepCard(
                    step = step,
                    number = index + 1,
                    style = StepCardStyle.Tile,
                    isDragging = isDragging,
                    onEvent = onEvent,
                    modifier = Modifier
                        .width(StepTileWidth)
                        .fillMaxHeight()
                        .animateItem()
                        .longPressDraggableHandle(onDragStarted = { haptic.performHapticFeedback(LongPress) }),
                )
            }
        }
        item(key = AddStepKey) {
            AddStepButton(
                onClick = { onEvent(AddStepClicked) },
                modifier = Modifier.fillMaxHeight().animateItem(),
            )
        }
        item(key = EndStepKey) {
            EndStepCard(
                finishTarget = finishTarget,
                onTypeClick = { onEvent(FinishTargetClicked(it)) },
                // The lazy row measures items with an unbounded width, so the card has to state an
                // intrinsic one for the equally weighted toggles inside it to resolve.
                modifier = Modifier.width(IntrinsicSize.Max).fillMaxHeight().animateItem(),
            )
        }
    }
}

@Composable
private fun StepCard(
    step: Step,
    number: Int,
    style: StepCardStyle,
    isDragging: Boolean,
    onEvent: (ProfileEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(if (isDragging) DraggingElevation else 0.dp)
    val icon = step.type.icon()
    val color = step.type.color()
    val value = step.valueLabel()
    val time = step.timeLabel()
    val removeStep = { onEvent(StepRemoved(step.id)) }

    Surface(
        onClick = { onEvent(StepClicked(step.id)) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = elevation,
        modifier = modifier,
    ) {
        when (style) {
            StepCardStyle.Row -> Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBadge(number = number, icon = icon, color = color)
                HorizontalSpacer(16.dp)
                Column(Modifier.weight(1f)) {
                    StepValueText(value)
                    StepTimeText(time)
                }
                RemoveStepButton(onClick = removeStep)
            }

            StepCardStyle.Tile -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StepBadge(number = number, icon = icon, color = color)
                    HorizontalSpacer(1f)
                    RemoveStepButton(onClick = removeStep)
                }
                VerticalSpacer(1f)
                StepValueText(value)
                StepTimeText(time)
            }
        }
    }
}

@Composable
private fun EndStepCard(
    finishTarget: FinishTarget,
    onTypeClick: (FinishTargetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepIcon(Icons.AutoMirrored.Filled.LastPage, MaterialTheme.colorScheme.primary)
                HorizontalSpacer(16.dp)
                Text(
                    text = stringResource(Res.string.profile_editor_end_step),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            VerticalSpacer(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinishTargetType.entries.forEach { type ->
                    FinishTargetButton(
                        type = type,
                        selected = finishTarget.type == type,
                        value = finishTarget.valueLabel(type),
                        onClick = { onTypeClick(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishTargetButton(
    type: FinishTargetType,
    selected: Boolean,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        if (selected) type.color() else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val containerColor by animateColorAsState(
        if (selected) type.color().disabled(SelectedContainerAlpha) else MaterialTheme.colorScheme.surfaceContainer,
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = modifier.height(EndToggleHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = rememberVectorPainter(type.icon()),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.fillMaxHeight(EndToggleIconFraction).aspectRatio(1f),
            )
            HorizontalSpacer(8.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AddStepButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Add),
                contentDescription = stringResource(Res.string.profile_editor_add_step),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun StepBadge(number: Int, icon: ImageVector, color: Color) {
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .squareSize()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = BadgeIconOffset, y = -BadgeIconOffset),
        ) {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(BadgeIconPadding).size(BadgeIconSize),
            )
        }
    }
}

@Composable
private fun StepIcon(icon: ImageVector, color: Color) = Icon(
    painter = rememberVectorPainter(icon),
    contentDescription = null,
    tint = color,
    modifier = Modifier.size(24.dp),
)

@Composable
private fun RemoveStepButton(onClick: () -> Unit) = IconButton(onClick = onClick) {
    Icon(
        painter = rememberVectorPainter(Icons.Filled.Close),
        contentDescription = stringResource(Res.string.profile_editor_remove_step),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun StepValueText(value: String) = Text(
    text = value,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurface,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun StepTimeText(time: String) = Text(
    text = time,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
