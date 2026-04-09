package dev.drobek.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Volume
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Weight
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_avg
import geeflow.core.ui.generated.resources.unit_bar
import geeflow.core.ui.generated.resources.unit_grams
import geeflow.core.ui.generated.resources.unit_grams_per_second
import geeflow.core.ui.generated.resources.unit_milliliters
import geeflow.core.ui.generated.resources.unit_milliliters_per_second
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewBar(
    brew: Brew,
    isBrewing: Boolean,
    visibleCharts: Set<DashboardChartType>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastPoint = brew.data.values.lastOrNull()

    val currentPressure = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.pressure }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.pressure?.toDouble() ?: 0.0
    }

    val currentFlow = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.volumePerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.volumePerSecond?.toDouble() ?: 0.0
    }

    val currentWeightRate = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.weightPerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.weightPerSecond?.toDouble() ?: 0.0
    }

    val totalWeight = lastPoint?.weight ?: 0f
    val totalVolume = lastPoint?.volume ?: 0f

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Text(
                text = brew.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${brew.time}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryItem(
                modifier = Modifier.weight(1f),
                values = listOf(
                    ValueEntry(
                        value = currentPressure,
                        unit = stringResource(Res.string.unit_bar),
                        color = MaterialTheme.colorScheme.error,
                        type = Pressure,
                        showDivider = visibleCharts.contains(Pressure),
                        showAvg = !isBrewing && brew.data.isNotEmpty()
                    )
                ),
                onToggle = onToggle
            )
            SummaryItem(
                modifier = Modifier.weight(2f),
                values = listOf(
                    ValueEntry(
                        value = currentWeightRate,
                        unit = stringResource(Res.string.unit_grams_per_second),
                        color = MaterialTheme.colorScheme.onSurface,
                        type = WeightRate,
                        showDivider = visibleCharts.contains(WeightRate),
                        showAvg = !isBrewing && brew.data.isNotEmpty()
                    ),
                    ValueEntry(
                        value = currentFlow,
                        unit = stringResource(Res.string.unit_milliliters_per_second),
                        color = GeeFlowTheme.colors.water,
                        type = FlowRate,
                        showDivider = visibleCharts.contains(FlowRate),
                        showAvg = !isBrewing && brew.data.isNotEmpty()
                    )
                ),
                onToggle = onToggle
            )
            SummaryItem(
                modifier = Modifier.weight(2f),
                values = listOf(
                    ValueEntry(
                        value = totalWeight.toDouble(),
                        unit = stringResource(Res.string.unit_grams),
                        color = MaterialTheme.colorScheme.onSurface,
                        type = Weight,
                        showDivider = visibleCharts.contains(Weight),
                        showAvg = false
                    ),
                    ValueEntry(
                        value = totalVolume.toDouble(),
                        unit = stringResource(Res.string.unit_milliliters),
                        color = GeeFlowTheme.colors.waterVariant,
                        type = Volume,
                        showDivider = visibleCharts.contains(Volume),
                        showAvg = false
                    )
                ),
                onToggle = onToggle
            )
        }
    }
}

private data class ValueEntry(
    val value: Double,
    val unit: String,
    val color: Color,
    val type: DashboardChartType,
    val showDivider: Boolean,
    val showAvg: Boolean
)

@Composable
private fun SummaryItem(
    values: List<ValueEntry>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        values.forEach { entry ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onToggle(entry.type) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = entry.value.format(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(
                    visible = entry.showDivider,
                    enter = expandIn(expandFrom = Alignment.Center) + fadeIn(),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 42.dp)
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .background(entry.color, CircleShape)
                            .height(2.dp)
                            .fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.unit,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedVisibility(
                        visible = entry.showAvg,
                        enter = expandIn(expandFrom = Alignment.CenterStart) + fadeIn(),
                        exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                    ) {
                        Text(
                            text = " ${stringResource(Res.string.common_avg)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun Double.format() = ((this * 10).toInt() / 10.0).toString()

private val previewBrew = Brew(
    name = "Slayer shot",
    time = 25,
    data = mapOf(
        1f to ChartData(
            pressure = 9.0f,
            weight = 36.0f,
            weightPerSecond = 2.5f,
            volume = 40.0f,
            volumePerSecond = 2.8f
        )
    )
)

private val previewVisibleCharts = setOf(
    Pressure,
    FlowRate,
    WeightRate
)

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    BrewBar(
        brew = previewBrew,
        isBrewing = true,
        visibleCharts = previewVisibleCharts,
        onToggle = {}
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    BrewBar(
        brew = previewBrew,
        isBrewing = false,
        visibleCharts = previewVisibleCharts,
        onToggle = {},
    )
}
