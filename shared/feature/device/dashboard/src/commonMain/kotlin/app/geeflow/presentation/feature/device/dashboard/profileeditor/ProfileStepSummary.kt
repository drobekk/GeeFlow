package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.icons.Experiment
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_title
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_default_name
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private const val IndicatorBackgroundAlpha = 0.12f

@Composable
internal fun ProfileStepSummary(step: Step, number: Int, modifier: Modifier = Modifier) {
    val duration = step.exitConditions.firstOrNull { it.metric == BrewMetric.PhaseTime }?.threshold
        ?: step.timeSec.toFloat()
    val durationLabel = if (duration % 1f == 0f) duration.toInt().toString() else duration.formatValue()

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = step.phaseName.ifBlank { stringResource(Res.string.step_editor_default_name, number) },
                modifier = Modifier.weight(weight = 1f, fill = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (step.experimental) {
                Icon(
                    imageVector = GeeFlowIcon.Experiment,
                    contentDescription = stringResource(Res.string.experimental_title),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = step.valueLabel(),
                modifier = Modifier.weight(weight = 1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$durationLabel ${stringResource(CoreRes.string.common_sec)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        VerticalSpacer(4.dp)
        StepIndicators(step = step)
    }
}

@Composable
private fun StepIndicators(step: Step) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepIndicator(
            icon = Icons.Outlined.Timer,
            label = BrewMetric.PhaseTime.conditionLabel(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (step.ramp.style != RampStyle.Instant) {
            StepIndicator(
                icon = Icons.Outlined.Timeline,
                label = stringResource(Res.string.experimental_ramp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        step.exitConditions.map { it.metric }.distinct().filter { it != BrewMetric.PhaseTime }.forEach { metric ->
            StepIndicator(
                icon = metric.icon(),
                label = metric.conditionLabel(),
                color = metric.indicatorColor(),
            )
        }
    }
}

@Composable
private fun StepIndicator(icon: ImageVector, label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(color = color.copy(alpha = IndicatorBackgroundAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            tint = color,
        )
    }
}

@Composable
private fun BrewMetric.indicatorColor(): Color = when (this) {
    BrewMetric.PhaseTime -> MaterialTheme.colorScheme.onSurfaceVariant
    BrewMetric.PumpedVolume -> GeeFlowTheme.colors.waterVariant
    BrewMetric.CupWeight -> MaterialTheme.colorScheme.tertiary
    BrewMetric.PumpFlow -> GeeFlowTheme.colors.water
    BrewMetric.PumpPressure, BrewMetric.GroupPressure, BrewMetric.BoilerPressure -> MaterialTheme.colorScheme.error
}
