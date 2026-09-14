@file:Suppress("TooManyFunctions")

package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.AddStepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepRemoved
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepsReordered
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.LineEndCircle
import app.geeflow.ui.modifier.squareSize
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.common_off
import geeflow.shared.core.ui.generated.resources.common_on
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_copy
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_add_step
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_end_step
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_target_disabled
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_remove_step
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private val CardSpacing = 8.dp
internal val StepTileWidth = 220.dp
private val EndToggleHeight = 60.dp
private val BadgeIconSize = 14.dp
private val BadgeIconOffset = 8.dp
private val BadgeIconPadding = 3.dp
private val DraggingElevation = 6.dp
private const val EndToggleIconFraction = 0.4f
private val SelectedBorderWidth = 2.dp

private const val AddStepKey = "add_step"
private const val EndStepKey = "end_step"

internal enum class StepCardStyle {
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
    allowAdd: Boolean = true,
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
        if (allowAdd) {
            item(key = AddStepKey) {
                AddStepButton(
                    onClick = { onEvent(AddStepClicked) },
                    modifier = Modifier.fillMaxWidth().animateItem(),
                )
            }
        }
        item(key = EndStepKey) {
            EndStepCard(
                finishTarget = finishTarget,
                onTypeClick = { onEvent(FinishTargetClicked(it)) },
                onToggle = { onEvent(ProfileEditorEvent.ToggleGlobalGoal) },
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
    allowAdd: Boolean = true,
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
        if (allowAdd) {
            item(key = AddStepKey) {
                AddStepButton(
                    onClick = { onEvent(AddStepClicked) },
                    modifier = Modifier.fillMaxHeight().animateItem(),
                )
            }
        }
        item(key = EndStepKey) {
            EndStepCard(
                finishTarget = finishTarget,
                onTypeClick = { onEvent(FinishTargetClicked(it)) },
                onToggle = { onEvent(ProfileEditorEvent.ToggleGlobalGoal) },
                // The lazy row measures items with an unbounded width, so the card has to state an
                // intrinsic one for the equally weighted toggles inside it to resolve.
                modifier = Modifier.width(IntrinsicSize.Max).fillMaxHeight().animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepCard(
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

    when (style) {
        StepCardStyle.Row -> Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onEvent(StepClicked(step.id)) }
                    .padding(start = 16.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBadge(number = number, icon = icon, color = color)
                HorizontalSpacer(16.dp)
                ProfileStepSummary(
                    step = step,
                    number = number,
                    modifier = Modifier.weight(1f),
                )
            }

            StepActions(
                modifier = Modifier.align(Alignment.CenterVertically),
                onRemove = { onEvent(StepRemoved(step.id)) },
                onDuplicate = { onEvent(ProfileEditorEvent.StepDuplicated(step.id)) },
            )
        }

        StepCardStyle.Tile -> Row(
            modifier = modifier
                .height(IntrinsicSize.Min)
                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onEvent(StepClicked(step.id)) }
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            ) {
                StepBadge(number = number, icon = icon, color = color)
                VerticalSpacer(1f)
                ProfileStepSummary(
                    step = step,
                    number = number,
                )
            }
            StepActions(
                modifier = Modifier.align(Alignment.CenterVertically),
                onRemove = { onEvent(StepRemoved(step.id)) },
                onDuplicate = { onEvent(ProfileEditorEvent.StepDuplicated(step.id)) },
            )
        }
    }
}

@Composable
private fun StepActions(
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onRemove,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Close),
                contentDescription = stringResource(Res.string.profile_editor_remove_step),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onDuplicate,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.ContentCopy),
                contentDescription = stringResource(Res.string.experimental_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun EndStepCard(
    finishTarget: FinishTarget,
    onTypeClick: (FinishTargetType) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = rememberVectorPainter(GeeFlowIcon.LineEndCircle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape)
                    .padding(5.dp)
                    .size(20.dp),
            )
            HorizontalSpacer(16.dp)
            Text(
                text = stringResource(Res.string.profile_editor_end_step),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalSpacer(1f)
            val buttonColor by animateColorAsState(
                if (finishTarget.enabled) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            )
            HorizontalSpacer(16.dp)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color = buttonColor)
                    .clickable(onClick = onToggle, role = Role.Button)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (finishTarget.enabled) CoreRes.string.common_on else CoreRes.string.common_off),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        VerticalSpacer(16.dp)
        AnimatedContent(targetState = finishTarget.enabled) { enabled ->
            if (enabled) {
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
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 200.dp),
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Outlined.Info),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    HorizontalSpacer(12.dp)
                    Text(
                        text = stringResource(Res.string.profile_editor_finish_target_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    // Only the border carries the target's colour; the icon and labels stay in the normal palette.
    val borderColor by animateColorAsState(if (selected) type.color() else Color.Transparent)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(SelectedBorderWidth, borderColor),
        modifier = modifier.height(EndToggleHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = rememberVectorPainter(type.icon()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxHeight(EndToggleIconFraction).aspectRatio(1f),
            )
            HorizontalSpacer(8.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
private fun StepBadge(
    number: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(top = BadgeIconOffset)) {
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

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun ProfileStepsColumnPreview() {
    ProfileStepsColumn(
        steps = PreviewSteps,
        finishTarget = FinishTarget(type = FinishTargetType.Weight, weight = 36f),
        onEvent = {},
        modifier = Modifier.width(360.dp).height(560.dp),
        contentPadding = PaddingValues(16.dp),
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun ProfileStepsRowPreview() {
    ProfileStepsRow(
        steps = PreviewSteps,
        finishTarget = FinishTarget(type = FinishTargetType.Weight, weight = 36f),
        onEvent = {},
        modifier = Modifier.width(1000.dp).height(220.dp),
        contentPadding = PaddingValues(16.dp),
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun StepCardRowPreview() {
    val step = Step(
        id = 1,
        type = StepType.Pressure,
        experimental = true,
        timeSec = 20,
        value = 9f,
        phaseName = "Extraction",
        ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 3000),
        exitConditions = listOf(
            ExitCondition(metric = BrewMetric.PhaseTime, comparison = ThresholdComparison.Above, threshold = 20f),
            ExitCondition(metric = BrewMetric.CupWeight, comparison = ThresholdComparison.Above, threshold = 36f),
            ExitCondition(metric = BrewMetric.PumpFlow, comparison = ThresholdComparison.Above, threshold = 4f),
        ),
    )
    Box(modifier = Modifier.padding(16.dp).width(320.dp)) {
        StepCard(
            step = step,
            number = 1,
            style = StepCardStyle.Row,
            isDragging = false,
            onEvent = {},
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun UnnamedStepCardRowPreview() {
    Box(modifier = Modifier.padding(16.dp).width(280.dp)) {
        StepCard(
            step = Step(id = 2, type = StepType.Flow, timeSec = 10, value = 4.5f),
            number = 2,
            style = StepCardStyle.Row,
            isDragging = false,
            onEvent = {},
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun StepCardTilePreview() {
    val step = Step(
        id = 1,
        type = StepType.Flow,
        experimental = true,
        timeSec = 10,
        value = 4.5f,
        phaseName = "Pre-infusion",
    )
    Box(modifier = Modifier.padding(16.dp).width(StepTileWidth).height(132.dp)) {
        StepCard(
            step = step,
            number = 1,
            style = StepCardStyle.Tile,
            isDragging = false,
            onEvent = {},
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun EndStepCardPreview() {
    val finishTarget = FinishTarget(
        enabled = true,
        type = FinishTargetType.Weight,
    )
    Box(modifier = Modifier.padding(16.dp).width(320.dp)) {
        EndStepCard(
            finishTarget = finishTarget,
            onTypeClick = {},
            onToggle = {},
        )
    }
}

private val PreviewSteps = listOf(
    Step(
        id = 1,
        type = StepType.Flow,
        timeSec = 10,
        value = 4.5f,
        phaseName = "Pre-infusion",
    ),
    Step(
        id = 2,
        type = StepType.Wait,
        timeSec = 5,
        value = 0f,
        phaseName = "Bloom",
    ),
    Step(
        id = 3,
        type = StepType.Pressure,
        timeSec = 25,
        value = 9f,
        phaseName = "Extraction",
        ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 3000),
        exitConditions = listOf(
            ExitCondition(metric = BrewMetric.PhaseTime, comparison = ThresholdComparison.Above, threshold = 25f),
            ExitCondition(metric = BrewMetric.CupWeight, comparison = ThresholdComparison.Above, threshold = 36f),
        ),
        experimental = true,
    ),
)
