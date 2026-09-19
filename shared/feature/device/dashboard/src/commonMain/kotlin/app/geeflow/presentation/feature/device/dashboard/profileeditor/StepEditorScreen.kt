package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.components.GeeFlowSlider
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_go_back
import geeflow.shared.core.ui.generated.resources.common_ok
import geeflow.shared.core.ui.generated.resources.common_save
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_add_condition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_measurement
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp_duration
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_remove_condition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_start_from
import geeflow.shared.feature.device.dashboard.generated.resources.profile_experimental_info
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_condition_and
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_condition_or
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_conditions
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_default_name
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_pause_hint
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_pump_control
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_rename
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_time_limit
import geeflow.shared.feature.device.dashboard.generated.resources.step_unit_flow
import geeflow.shared.feature.device.dashboard.generated.resources.step_unit_pressure
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

/** The provider is a child of ProfileEditor's owner and survives its configuration changes. */
@Composable
internal fun ScopedStepEditor(
    request: StepEditorRequest,
    parent: ProfileEditorViewState,
    onSaved: (ProfileEditorViewState.Step) -> Unit,
    onCancel: () -> Unit,
) {
    val provider = rememberViewModelStoreProvider(key = request.key)
    val owner = rememberViewModelStoreOwner(key = request.key, provider = provider)
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        val editor = viewModel {
            StepEditorViewModel(
                step = request.step,
                capabilities = parent.profilingCapabilities,
                pressureRange = parent.pressureRange,
                flowRange = parent.flowRange,
                stepNumber = if (request.isNew) {
                    parent.steps.size + 1
                } else {
                    parent.steps.indexOfFirst { it.id == request.step.id } + 1
                },
            )
        }
        StepEditorScreen(viewModel = editor, onSaved = onSaved, onCancel = onCancel)
    }
}

@Composable
private fun StepEditorScreen(
    viewModel: StepEditorViewModel,
    onSaved: (ProfileEditorViewState.Step) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val explanation = stringResource(Res.string.profile_experimental_info)
    val ok = stringResource(CoreRes.string.common_ok)
    if (LocalNavigationEventDispatcherOwner.current != null) {
        NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) { onCancel() }
    }
    EventsDispatcher(viewModel.events) { event ->
        when (event) {
            is StepEditorEffect.Saved -> onSaved(event.step)
            StepEditorEffect.Cancelled -> onCancel()
            StepEditorEffect.ExplainExperimental -> scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(explanation, actionLabel = ok, duration = SnackbarDuration.Indefinite)
            }
        }
    }
    StepEditorContent(state = state, snackbar = snackbar, onEvent = viewModel::handleEvent)
}

@Composable
internal fun StepEditorContent(
    state: StepEditorViewState,
    snackbar: SnackbarHostState,
    onEvent: (StepEditorEvent) -> Unit,
) {
    val title = state.name.ifBlank { stringResource(Res.string.step_editor_default_name, state.stepNumber) }
    if (state.renaming) RenameStepDialog(name = title, onEvent = onEvent)
    if (state.input != null) StepInputDialog(state = state, onEvent = onEvent)
    Scaffold(
        contentWindowInsets = GeeFlowInsets.content,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(GeeFlowInsets.top)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(onClick = { onEvent(StepEditorEvent.Cancel) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(CoreRes.string.common_go_back),
                    )
                }
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { onEvent(StepEditorEvent.RenameClicked) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.step_editor_rename),
                        )
                    }
                }
                if (state.isExperimental) {
                    ExperimentButton(tonal = true, onClick = { onEvent(StepEditorEvent.ExperimentClicked) })
                }
                Button(onClick = { onEvent(StepEditorEvent.Save) }, enabled = state.toStep() != null) {
                    Text(text = stringResource(CoreRes.string.common_save))
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
    ) { padding ->
        if (isWidthExpanded()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    item {
                        ControlPane(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    conditionItems(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                item {
                    ControlPane(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { VerticalSpacer(10.dp) }
                conditionItems(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPane(
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        VerticalSpacer(16.dp)
        Text(
            text = stringResource(Res.string.step_editor_pump_control),
            style = MaterialTheme.typography.titleMedium,
        )
        VerticalSpacer(16.dp)
        val types = listOf(StepType.Pressure, StepType.Flow, StepType.Wait)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.type == type,
                    enabled = state.controlSupported(type),
                    onClick = { onEvent(StepEditorEvent.TypeChanged(type)) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size, baseShape = MaterialTheme.shapes.large),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        activeBorderColor = Color.Transparent,
                        inactiveBorderColor = Color.Transparent,
                    ),
                    border = BorderStroke(0.dp, Color.Transparent),
                    icon = {
                        Icon(
                            imageVector = type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                ) {
                    Text(text = type.label())
                }
            }
        }
        VerticalSpacer(12.dp)
        if (state.type != StepType.Wait) {
            GeeFlowSlider(
                value = state.target.toFloatOrNull() ?: state.targetRange.start,
                onValueChange = { onEvent(StepEditorEvent.TargetChanged(it.toString())) },
                valueRange = state.targetRange,
                unit = stringResource(
                    if (state.type == StepType.Pressure) Res.string.step_unit_pressure else Res.string.step_unit_flow,
                ),
                color = state.type.color(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SliderHeight),
                onClick = { onEvent(StepEditorEvent.InputClicked(StepInput.Target)) },
            )
            VerticalSpacer(16.dp)
            Text(
                text = stringResource(Res.string.experimental_ramp),
                style = MaterialTheme.typography.titleMedium,
            )
            VerticalSpacer(12.dp)
            RampSelector(state = state, onEvent = onEvent, modifier = Modifier.fillMaxWidth())
            if (state.rampStyle != RampStyle.Instant) {
                VerticalSpacer(12.dp)
                EditorField(value = state.rampSeconds, label = stringResource(Res.string.experimental_ramp_duration)) {
                    onEvent(StepEditorEvent.InputClicked(StepInput.RampDuration))
                }
                VerticalSpacer(12.dp)
                EditorChoice(
                    title = stringResource(resource = Res.string.experimental_start_from),
                    values = RampStart.entries,
                    selected = state.rampStart,
                    label = { enumLabel(it) },
                    onSelect = { onEvent(StepEditorEvent.RampStartChanged(it)) },
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.step_editor_pause_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

private fun LazyListScope.conditionItems(
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
) {
    item(key = "time-limit-heading") {
        VerticalSpacer(16.dp)
        Text(
            text = stringResource(Res.string.step_editor_time_limit),
            style = MaterialTheme.typography.titleMedium,
        )
        VerticalSpacer(16.dp)
    }
    items(items = state.conditions.filter { it.metric == BrewMetric.PhaseTime }, key = { it.id }) { condition ->
        ConditionTile(
            condition = condition,
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
    }
    item(key = "conditions-heading") {
        ConditionsHeading(state, onEvent)
        VerticalSpacer(16.dp)
    }
    items(items = state.conditions.filterNot { it.metric == BrewMetric.PhaseTime }, key = { it.id }) { condition ->
        ConditionTile(condition = condition, state = state, onEvent = onEvent, modifier = Modifier.fillMaxWidth())
        VerticalSpacer(16.dp)
    }
    item(key = "add-condition") {
        AddConditionButton(
            onEvent = onEvent,
            enabled = state.availableConditionMetrics.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConditionsHeading(
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.step_editor_conditions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = { onEvent(StepEditorEvent.ConditionOperatorToggled) },
            modifier = Modifier
                .defaultMinSize(minHeight = 0.dp)
                .height(26.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.conditionOperator == ConditionOperator.And) {
                        Res.string.step_editor_condition_and
                    } else {
                        Res.string.step_editor_condition_or
                    },
                ),
            )
        }
    }
}

@Composable
private fun ConditionTile(
    condition: ConditionDraft,
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
    modifier: Modifier,
) {
    val canRemove = condition.metric != BrewMetric.PhaseTime
    if (canRemove) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConditionContent(
                condition = condition,
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            IconButton(
                onClick = { onEvent(StepEditorEvent.ConditionRemoved(condition.id)) },
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.experimental_remove_condition),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    } else {
        ConditionContent(
            condition = condition,
            state = state,
            onEvent = onEvent,
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun ConditionContent(
    condition: ConditionDraft,
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorChoice(
            title = stringResource(Res.string.experimental_measurement),
            values = if (condition.metric == BrewMetric.PhaseTime) {
                listOf(BrewMetric.PhaseTime)
            } else {
                state.availableConditionMetrics
            },
            selected = condition.metric,
            modifier = Modifier.fillMaxWidth(),
            label = { it.conditionLabel() },
            icon = { it.icon() },
            experimental = { state.conditionExperimental(it) },
            onExplain = { onEvent(StepEditorEvent.ExperimentClicked) },
            onSelect = {
                onEvent(
                    StepEditorEvent.ConditionChanged(
                        condition.copy(
                            metric = it,
                            comparison = ThresholdComparison.Above,
                        ),
                    ),
                )
            },
        )
        if (condition.metric in PressureAndFlowMetrics) {
            FilledTonalButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    val comparison = if (condition.comparison == ThresholdComparison.Above) {
                        ThresholdComparison.Below
                    } else {
                        ThresholdComparison.Above
                    }
                    onEvent(StepEditorEvent.ConditionChanged(condition.copy(comparison = comparison)))
                },
            ) { Text(text = enumLabel(condition.comparison)) }
        }
        EditorField(value = condition.value, label = condition.metric.thresholdLabel()) {
            onEvent(StepEditorEvent.InputClicked(StepInput.Condition(condition.id)))
        }
    }
}

@Composable
private fun AddConditionButton(
    onEvent: (StepEditorEvent) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onEvent(StepEditorEvent.ConditionAdded) },
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.experimental_add_condition),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    StepEditorContent(
        state = StepEditorViewState(
            source = ProfileEditorViewState.Step(
                id = 1,
                type = StepType.Pressure,
                timeSec = 25,
                value = 9f,
                phaseName = "Extraction",
            ),
            capabilities = ProfilingCapabilities(),
            pressureRange = 0f..12f,
            flowRange = 0f..8f,
        ),
        snackbar = remember { SnackbarHostState() },
        onEvent = {},
    )
}

private val SliderHeight = 64.dp
