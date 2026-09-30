package app.geeflow.presentation.feature.device.dashboard.maintenance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.device.model.CleaningType
import app.geeflow.domain.device.AutoFlushCoordinator
import app.geeflow.platform.ForegroundEffect
import app.geeflow.ui.EventsDispatcher
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MaintenanceReminderHost(
    deviceId: Long,
    blocked: Boolean,
    onOpen: (CleaningType) -> Unit,
    onError: () -> Unit,
) {
    val viewModel = koinViewModel<MaintenanceReminderViewModel>(key = "maintenance-$deviceId") {
        parametersOf(deviceId)
    }
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val autoFlush = koinInject<AutoFlushCoordinator>()
    val countdown by autoFlush.countdown.collectAsStateWithLifecycle()
    var active by remember { mutableStateOf(false) }
    val visible = state.eligible && state.dueTypes.isNotEmpty() && countdown == null

    LifecycleResumeEffect(viewModel) {
        active = true
        viewModel.resumed()
        onPauseOrDispose { active = false }
    }
    ForegroundEffect(viewModel) { viewModel.resumed() }
    EventsDispatcher(viewModel.events) { onError() }

    if (active && !blocked && visible) {
        MaintenanceReminderDialog(
            state = state,
            onOpen = onOpen,
            onSkip = { viewModel.skip(state.dueTypes) },
        )
    }
}
