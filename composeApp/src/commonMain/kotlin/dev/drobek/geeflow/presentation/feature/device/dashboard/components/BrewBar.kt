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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.FlowRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Pressure
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Volume
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Weight
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.WeightRate
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.unit_bar
import geeflow.composeapp.generated.resources.unit_grams
import geeflow.composeapp.generated.resources.unit_grams_per_second
import geeflow.composeapp.generated.resources.unit_milliliters
import geeflow.composeapp.generated.resources.unit_milliliters_per_second
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewBar(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastPoint = brew.data.values.lastOrNull()
    val currentPressure = lastPoint?.pressure?.toDouble() ?: 0.0
    val currentFlow = lastPoint?.volumePerSecond?.toDouble() ?: 0.0
    val currentWeightRate = lastPoint?.weightPerSecond?.toDouble() ?: 0.0
    val totalWeight = lastPoint?.weight ?: 0f
    val totalVolume = lastPoint?.volume ?: 0f

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Slayer shot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${brew.time.toInt()}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        showDivider = visibleCharts.contains(Pressure)
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
                        showDivider = visibleCharts.contains(WeightRate)
                    ),
                    ValueEntry(
                        value = currentFlow,
                        unit = stringResource(Res.string.unit_milliliters_per_second),
                        color = GeeFlowTheme.colors.water,
                        type = FlowRate,
                        showDivider = visibleCharts.contains(FlowRate)
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
                        showDivider = visibleCharts.contains(Weight)
                    ),
                    ValueEntry(
                        value = totalVolume.toDouble(),
                        unit = stringResource(Res.string.unit_milliliters),
                        color = GeeFlowTheme.colors.waterVariant,
                        type = Volume,
                        showDivider = visibleCharts.contains(Volume)
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
    val showDivider: Boolean
)

@Composable
private fun SummaryItem(
    values: List<ValueEntry>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .background(entry.color, CircleShape)
                            .height(2.dp)
                            .fillMaxWidth(),
                    )
                }
                Text(
                    text = entry.unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Double.format() = ((this * 10).toInt() / 10.0).toString()

private val previewBrew = Brew(
    name = "Slayer shot",
    time = 25f,
    data = mapOf(
        1f to Brew.Data(
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
        visibleCharts = previewVisibleCharts,
        onToggle = {}
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    BrewBar(
        brew = previewBrew,
        visibleCharts = previewVisibleCharts,
        onToggle = {},
    )
}
