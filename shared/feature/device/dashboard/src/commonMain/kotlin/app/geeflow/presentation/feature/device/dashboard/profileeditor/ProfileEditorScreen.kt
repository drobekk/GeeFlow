package app.geeflow.presentation.feature.device.dashboard.profileeditor

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsets
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_go_back
import geeflow.shared.core.ui.generated.resources.common_save
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_rename_title
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

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ProfileEditorViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    ProfileEditorContent(
        viewState = viewState,
        snackbarState = snackbarState,
        onEvent = viewModel::handleEvent,
    )

    viewState.dialog?.let { dialog ->
        ProfileEditorDialogs(
            dialog = dialog,
            onEvent = viewModel::handleEvent,
            profileName = viewState.profileName,
            pressureRange = viewState.pressureRange,
            flowRange = viewState.flowRange,
        )
    }
}

@Composable
private fun ProfileEditorContent(
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
                    .navigationBarsPadding(),
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
                profileName = viewState.profileName,
                canSave = viewState.canSave,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            ProfileStepsChart(
                targetData = viewState.targetData,
                modifier = Modifier.weight(1f).padding(bottom = 16.dp),
            )
        }
        ProfileStepsColumn(
            steps = viewState.steps,
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            ProfileEditorTopBar(
                profileName = viewState.profileName,
                canSave = viewState.canSave,
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
            ProfileStepsChart(
                targetData = viewState.targetData,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            )
            ProfileStepsRow(
                steps = viewState.steps,
                finishTarget = viewState.finishTarget,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .height(StepsRowHeight),
                contentPadding = PaddingValues(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun ProfileEditorTopBar(
    profileName: String,
    canSave: Boolean,
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
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profileName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            RenameButton(onClick = { onEvent(RenameClicked) })
        }
        HorizontalSpacer(8.dp)
        Button(onClick = { onEvent(SaveClicked) }, enabled = canSave) {
            Text(stringResource(CoreRes.string.common_save))
        }
    }
}

@Composable
private fun RenameButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp).size(32.dp),
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Filled.Edit),
            contentDescription = stringResource(Res.string.profile_editor_rename_title),
            modifier = Modifier.size(16.dp),
        )
    }
}

private val StepsColumnWidth = 320.dp
private val StepsRowHeight = 132.dp

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    val steps = listOf(
        Step(id = 0, type = StepType.Flow, timeSec = 5, value = 6f),
        Step(id = 1, type = StepType.Wait, timeSec = 2, value = 0f),
        Step(id = 2, type = StepType.Pressure, timeSec = 20, value = 9f),
    )
    ProfileEditorContent(
        viewState = ProfileEditorViewState(
            profileName = "Zuppa",
            steps = steps,
            finishTarget = FinishTarget(type = FinishTargetType.Weight, weight = 60f),
            targetData = steps.map { it.toDomain() }.toTargetData(),
        ),
        snackbarState = remember { SnackbarHostState() },
        onEvent = {},
    )
}
