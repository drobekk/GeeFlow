package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.ui.icons.FlowControl
import app.geeflow.ui.icons.Flush
import app.geeflow.ui.icons.GeeFlowIcon
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_hot_water
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_screen_title
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.feature.device.dashboard.generated.resources.Res as DashboardRes

@Composable
internal fun DashboardBrewButton(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildList {
        add(
            BrewButtonAction(
                contentDescription = stringResource(Res.string.common_flush),
                onClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
            ) {
                Icon(rememberVectorPainter(GeeFlowIcon.Flush), contentDescription = null)
            },
        )
        add(
            BrewButtonAction(
                contentDescription = stringResource(DashboardRes.string.free_control_screen_title),
                onClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
            ) {
                Icon(rememberVectorPainter(GeeFlowIcon.FlowControl), contentDescription = null)
            },
        )
        if (viewState.hotWaterSupported) {
            add(
                BrewButtonAction(
                    contentDescription = stringResource(Res.string.common_hot_water),
                    onClick = { onEvent(DeviceDashboardEvent.HotWaterClicked) },
                ) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null)
                },
            )
        }
    }
    BrewButton(
        state = viewState.brewButtonState,
        sideActions = actions,
        onHeroButtonClick = { onEvent(DeviceDashboardEvent.BrewClicked) },
        onStopClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
        modifier = modifier,
    )
}
