package app.geeflow.presentation.feature.device.dashboard.freecontrol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.geeflow.presentation.feature.device.dashboard.components.BrewButton
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ModeChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameConfirmed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameDismissed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StartClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StopClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.TargetChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.GeeFlowSlider
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.icons.Flow
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_cancel
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_go_back
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_default_profile_name
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_rename_dialog_name
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_rename_dialog_title
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_save
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_screen_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private const val PressureMin = 0f
private const val PressureMax = 12f
private const val FlowMin = 0f
private const val FlowMax = 8f
private val SliderColumnWidth = 112.dp
private val SliderHeight = 72.dp

@Composable
internal fun FreeControlScreen(
    viewModel: FreeControlViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val defaultProfileName = stringResource(Res.string.free_control_default_profile_name)

    LaunchedEffect(Unit) {
        viewModel.handleEvent(RenameConfirmed(defaultProfileName))
    }

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is FreeControlViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    FreeControlContent(viewState = viewState, snackbarState = snackbarState, onEvent = viewModel::handleEvent)

    if (viewState.showRenameDialog) {
        RenameProfileDialog(
            currentName = viewState.profileName,
            onConfirm = { viewModel.handleEvent(RenameConfirmed(it)) },
            onDismiss = { viewModel.handleEvent(RenameDismissed) },
        )
    }
}

@Composable
private fun FreeControlContent(
    viewState: FreeControlViewState,
    snackbarState: SnackbarHostState,
    onEvent: (FreeControlEvent) -> Unit,
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
    viewState: FreeControlViewState,
    onEvent: (FreeControlEvent) -> Unit,
) {
    val isPressure = viewState.mode == ControlMode.Pressure
    val pressureColor = MaterialTheme.colorScheme.error
    val flowColor = GeeFlowTheme.colors.water
    val sliderValue = if (isPressure) viewState.pressureTarget else viewState.flowTarget
    val sliderUnit = stringResource(
        if (isPressure) {
            CoreRes.string.unit_bar
        } else {
            CoreRes.string.unit_milliliters_per_second
        },
    )
    val sliderColor = if (isPressure) pressureColor else flowColor
    val sliderRange = if (isPressure) PressureMin..PressureMax else FlowMin..FlowMax

    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp),
        ) {
            FreeControlTopBar(
                sessionCompleted = viewState.sessionCompleted,
                brewStatus = viewState.brewStatus,
                onBack = { onEvent(BackClicked) },
                onSave = { onEvent(SaveClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
            )
            BrewCharts(
                brew = viewState.brew,
                visibleCharts = viewState.visibleCharts,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                BrewBar(
                    brew = viewState.brew.copy(name = viewState.profileName),
                    isBrewing = viewState.brewStatus == FreeBrewStatus.Active,
                    visibleCharts = viewState.visibleCharts,
                    onToggle = { onEvent(ToggleChartVisibility(it)) },
                    onRename = { onEvent(RenameClicked) },
                    modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                )
                HorizontalSpacer(16.dp)
                BrewButton(
                    isBrewing = viewState.brewStatus == FreeBrewStatus.Active,
                    onManualClick = { onEvent(ModeChanged(ControlMode.Pressure)) },
                    onFlowClick = { onEvent(StartClicked) },
                    onManualFlowClick = { onEvent(ModeChanged(ControlMode.Flow)) },
                    onStopClick = { onEvent(StopClicked) },
                    startIcon = {
                        Icon(
                            painter = rememberVectorPainter(GeeFlowIcon.Pressure),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isPressure) pressureColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    endIcon = {
                        Icon(
                            painter = rememberVectorPainter(GeeFlowIcon.Flow),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (!isPressure) flowColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }

        GeeFlowSlider(
            value = sliderValue,
            onValueChange = { onEvent(TargetChanged(it)) },
            valueRange = sliderRange,
            unit = sliderUnit,
            color = sliderColor,
            vertical = true,
            modifier = Modifier
                .width(SliderColumnWidth)
                .fillMaxHeight()
                .padding(16.dp),
        )
    }
}

@Composable
private fun CompactLayout(
    viewState: FreeControlViewState,
    snackbarState: SnackbarHostState,
    onEvent: (FreeControlEvent) -> Unit,
) {
    val isPressure = viewState.mode == ControlMode.Pressure
    val pressureColor = MaterialTheme.colorScheme.error
    val flowColor = GeeFlowTheme.colors.water
    val sliderValue = if (isPressure) viewState.pressureTarget else viewState.flowTarget
    val sliderUnit = stringResource(
        if (isPressure) {
            CoreRes.string.unit_bar
        } else {
            CoreRes.string.unit_milliliters_per_second
        },
    )
    val sliderColor = if (isPressure) pressureColor else flowColor
    val sliderRange = if (isPressure) PressureMin..PressureMax else FlowMin..FlowMax

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            FreeControlTopBar(
                sessionCompleted = viewState.sessionCompleted,
                brewStatus = viewState.brewStatus,
                onBack = { onEvent(BackClicked) },
                onSave = { onEvent(SaveClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        },
        bottomBar = {
            BrewButton(
                isBrewing = viewState.brewStatus == FreeBrewStatus.Active,
                onManualClick = { onEvent(ModeChanged(ControlMode.Pressure)) },
                onFlowClick = { onEvent(StartClicked) },
                onManualFlowClick = { onEvent(ModeChanged(ControlMode.Flow)) },
                onStopClick = { onEvent(StopClicked) },
                startIcon = {
                    Icon(
                        painter = rememberVectorPainter(GeeFlowIcon.Pressure),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isPressure) pressureColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                endIcon = {
                    Icon(
                        painter = rememberVectorPainter(GeeFlowIcon.Flow),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (!isPressure) flowColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 18.dp),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            BrewCharts(
                brew = viewState.brew,
                visibleCharts = viewState.visibleCharts,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp),
            )
            BrewBar(
                brew = viewState.brew.copy(name = viewState.profileName),
                isBrewing = viewState.brewStatus == FreeBrewStatus.Active,
                visibleCharts = viewState.visibleCharts,
                onToggle = { onEvent(ToggleChartVisibility(it)) },
                onRename = { onEvent(RenameClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp),
            )
            GeeFlowSlider(
                value = sliderValue,
                onValueChange = { onEvent(TargetChanged(it)) },
                valueRange = sliderRange,
                unit = sliderUnit,
                color = sliderColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SliderHeight)
                    .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun FreeControlTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    sessionCompleted: Boolean,
    brewStatus: FreeBrewStatus,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(CoreRes.string.common_go_back),
            )
        }
        Text(
            text = stringResource(Res.string.free_control_screen_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSave,
            enabled = sessionCompleted && brewStatus == FreeBrewStatus.Idle,
            modifier = Modifier.padding(end = 8.dp),
        ) {
            Text(stringResource(Res.string.free_control_save))
        }
    }
}

@Composable
private fun RenameProfileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nameState = rememberTextFieldState(initialText = currentName)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.free_control_rename_dialog_title)) },
        text = {
            GeeFlowOutlinedTextField(
                state = nameState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.free_control_rename_dialog_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameState.text.toString()) },
                enabled = nameState.text.isNotBlank(),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreRes.string.common_cancel))
            }
        },
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    FreeControlContent(
        viewState = FreeControlViewState(
            deviceName = "GeeFlow Pro",
            profileName = "New freehand profile",
            mode = ControlMode.Pressure,
            pressureTarget = 6.0f,
            flowTarget = 3.5f,
            brewStatus = FreeBrewStatus.Idle,
            sessionCompleted = false,
            visibleCharts = setOf(
                DashboardChartType.Pressure,
                DashboardChartType.FlowRate,
                DashboardChartType.WeightRate,
            ),
        ),
        snackbarState = remember { SnackbarHostState() },
        onEvent = {},
    )
}
