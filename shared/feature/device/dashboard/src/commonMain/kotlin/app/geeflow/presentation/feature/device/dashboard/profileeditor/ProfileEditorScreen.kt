package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.components.BrewBar
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.model.chartModel
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.EditDetailsClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StopClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.TestClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.animations.BackwardTransition
import app.geeflow.ui.animations.ForwardTransition
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsets
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_go_back
import geeflow.shared.core.ui.generated.resources.common_ok
import geeflow.shared.core.ui.generated.resources.common_save
import geeflow.shared.core.ui.generated.resources.common_stop
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_test
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ProfileEditorScreen(
    viewModel: ProfileEditorViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val ok = stringResource(CoreRes.string.common_ok)

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ProfileEditorViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(
                    message = it.message,
                    actionLabel = if (it.persistent) ok else null,
                    duration = if (it.persistent) SnackbarDuration.Indefinite else SnackbarDuration.Short,
                )
            }
        }
    }

    AnimatedContent(
        targetState = viewState.stepEditor,
        transitionSpec = {
            if (targetState != null) {
                ForwardTransition
            } else {
                BackwardTransition
            }
        },
        label = "stepEditorTransition",
    ) { request ->
        if (request != null) {
            ScopedStepEditor(
                request,
                viewState,
                onSaved = { viewModel.handleEvent(ProfileEditorEvent.StepEditorSaved(it)) },
                onCancel = { viewModel.handleEvent(ProfileEditorEvent.StepEditorCancelled) },
            )
        } else {
            ProfileEditorContent(
                viewState = viewState,
                snackbarState = snackbarState,
                onEvent = viewModel::handleEvent,
            )
        }
    }

    viewState.dialog?.let { dialog ->
        ProfileEditorDialogs(
            dialog = dialog,
            viewState = viewState,
            onEvent = viewModel::handleEvent,
        )
    }
}

@Composable
internal fun ProfileEditorContent(
    viewState: ProfileEditorViewState,
    snackbarState: SnackbarHostState,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    if (isWidthExpanded()) {
        Box(Modifier.fillMaxSize()) {
            ExpandedLayout(viewState, onEvent)
            SnackbarHost(
                hostState = snackbarState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(GeeFlowInsets.bottom),
            )
        }
    } else {
        CompactLayout(viewState, snackbarState, onEvent)
    }
}

@Composable
private fun ExpandedLayout(
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    // Taken as padding values rather than inset modifiers: the steps list needs them in its content
    // padding to avoid being clipped, and both panes have to agree on the top inset for the save
    // button and the first step to stay aligned. Each pane takes only the outer edge it touches.
    val chartInsets = WindowInsets.geeFlowInsets
        .only(WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.Bottom)
        .asPaddingValues()
    val stepsInsets = WindowInsets.geeFlowInsets
        .only(WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom)
        .asPaddingValues()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(chartInsets)
                .padding(start = 16.dp),
        ) {
            ProfileEditorTopBar(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            BrewCharts(
                model = viewState.chartModel(),
                visibleCharts = viewState.visibleCharts,
                modifier = Modifier.weight(1f),
            )
            EditorBrewBar(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
        ProfileStepsColumn(
            steps = viewState.steps,
            allowAdd = !viewState.isRecording,
            finishTarget = viewState.finishTarget,
            onEvent = onEvent,
            modifier = Modifier
                .width(StepsColumnWidth)
                .fillMaxHeight(),
            contentPadding = stepsInsets + PaddingValues(16.dp),
        )
    }
}

@Composable
private fun CompactLayout(
    viewState: ProfileEditorViewState,
    snackbarState: SnackbarHostState,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    Scaffold(
        contentWindowInsets = GeeFlowInsets.content,
        modifier = Modifier
            .fillMaxSize()
            .geeFlowInsetsPadding(),
        topBar = {
            ProfileEditorTopBar(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            BrewCharts(
                model = viewState.chartModel(),
                visibleCharts = viewState.visibleCharts,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            )
            EditorBrewBar(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp),
            )
            ProfileStepsRow(
                steps = viewState.steps,
                allowAdd = !viewState.isRecording,
                finishTarget = viewState.finishTarget,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .height(150.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun EditorBrewBar(
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) = BrewBar(
    brew = viewState.brew.copy(name = viewState.profileName, description = viewState.displayDescription),
    isBrewing = viewState.isBrewing,
    visibleCharts = viewState.visibleCharts,
    onToggle = { onEvent(ToggleChartVisibility(it)) },
    onHeaderClick = { onEvent(EditDetailsClicked) },
    showEditIcon = true,
    modifier = modifier,
)

/** Brews the profile as edited without saving it first, and turns into a stop button while it runs. */
@Composable
private fun TestButton(
    isBrewing: Boolean,
    canTest: Boolean,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    Button(
        onClick = { onEvent(if (isBrewing) StopClicked else TestClicked) },
        enabled = isBrewing || canTest,
        colors = if (isBrewing) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
    ) {
        Text(stringResource(if (isBrewing) CoreRes.string.common_stop else Res.string.profile_editor_test))
    }
}

@Composable
private fun ProfileEditorTopBar(
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onEvent(BackClicked) }) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Close),
                contentDescription = stringResource(CoreRes.string.common_go_back),
            )
        }
        HorizontalSpacer(1f)
        if (viewState.experimental) {
            ExperimentButton(onClick = { onEvent(ProfileEditorEvent.ExperimentClicked) })
            HorizontalSpacer(8.dp)
        }
        TestButton(isBrewing = viewState.isBrewing, canTest = viewState.canTest, onEvent = onEvent)
        HorizontalSpacer(8.dp)
        Button(onClick = { onEvent(SaveClicked) }, enabled = viewState.canSave) {
            Text(stringResource(CoreRes.string.common_save))
        }
    }
}

private val StepsColumnWidth = 320.dp

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
internal fun ProfileEditorPreviewContent() {
    val steps = listOf(
        Step(id = 0, type = StepType.Pressure, timeSec = 5, value = 3f),
        Step(id = 1, type = StepType.Wait, timeSec = 5, value = 0f),
        Step(id = 2, type = StepType.Pressure, timeSec = 30, value = 6f),
    )
    ProfileEditorContent(
        viewState = ProfileEditorViewState(
            profileName = "Preinfusion",
            steps = steps,
            finishTarget = FinishTarget(type = FinishTargetType.Weight, weight = 36f),
            targetData = steps.map { it.toDomain() }.toTargetData(),
        ),
        snackbarState = remember { SnackbarHostState() },
        onEvent = {},
    )
}
