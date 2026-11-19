package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.user.model.FlowChartRange
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.presentation.feature.device.dashboard.main.historicalRecipeValues
import app.geeflow.presentation.feature.device.dashboard.main.shotDurationSeconds
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import geeflow.shared.core.ui.generated.resources.common_pressure
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_time
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_grams_per_second
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_disconnected
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_flow_range_exceeded
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_last_shot
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_live
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_output
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_ready
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_target_flow
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_cup_flow_label
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_flow_title
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_cup_rate
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round
import kotlin.math.roundToInt
import geeflow.shared.core.ui.generated.resources.Res as CoreRes
import geeflow.shared.feature.device.dashboard.generated.resources.Res as DashboardRes

@Composable
internal fun ExtractionReadout(
    state: DeviceDashboardViewState,
    profiles: List<ProfileListViewState.Profile>,
    onToggle: (DeviceDashboardViewState.DashboardChartType) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    columns: Int = if (compact) PhoneColumns else TabletColumns,
    cupFlowGramsPerSecond: Float? = null,
    scaleWeightGrams: Float? = null,
    scaleConnected: Boolean = false,
    targetWeightGrams: Float? = null,
    nativeTargetWeightGrams: Float? = null,
    liveSample: Map.Entry<Float, app.geeflow.presentation.feature.device.dashboard.model.ChartData>? = null,
) {
    val lastPoint = liveSample?.value ?: state.brew.data.entries.maxByOrNull { it.key }?.value
    val history = liveSample == null && state.readingsAreHistorical()
    val connected = state.device.connectionStatus == DeviceDashboardViewState.Device.ConnectionStatus.Connected
    val selectedProfile = profiles.firstOrNull { it.selected }
    val brewProfile = profiles.firstOrNull { state.brew.name.isNotBlank() && it.name == state.brew.name }
    val activeProfile = when {
        state.brew.historyId != null -> null // A current profile with the same name is not a historical snapshot.
        state.device.isBrewing || history -> brewProfile ?: selectedProfile.takeUnless { history }
        else -> selectedProfile
    }
    val flow = when {
        history || state.device.isBrewing || liveSample != null -> lastPoint?.volumePerSecond?.toDouble()
        connected -> 0.0
        else -> null
    }
    val targetFlow = activeProfile?.targetData?.values
        ?.maxOfOrNull { it.volumePerSecond.toDouble() }
        ?.takeIf { it > 0.0 }
    val flowRangeMax = FlowChartRange.TWELVE.maxMillilitersPerSecond
    val overRange = listOfNotNull(flow, targetFlow).maxOrNull()?.takeIf { it > flowRangeMax }
    val output = outputReading(
        state, activeProfile, lastPoint?.weight?.toDouble(), lastPoint?.volume?.toDouble(),
        (cupFlowGramsPerSecond ?: lastPoint?.weightPerSecond.takeIf { targetWeightGrams == null })
            .takeIf { history || scaleConnected || state.device.smartScaleConnected },
        scaleWeightGrams, scaleConnected, targetWeightGrams, nativeTargetWeightGrams.takeUnless { history },
    )
    val livePressure = if (!history && connected) state.device.pressure?.toDoubleOrNull() else null
    val pressure = if (history || liveSample != null) lastPoint?.pressure?.toDouble() else livePressure
    val cells = listOf<@Composable (Modifier) -> Unit>(
        { cellModifier ->
            MetricCard(
                label = stringResource(CoreRes.string.common_pressure),
                value = pressure?.formatOneDecimal() ?: "—",
                unit = stringResource(CoreRes.string.unit_bar),
                accent = BrewChartPalette.Pressure,
                selected = state.visibleCharts.contains(Pressure),
                onClick = { onToggle(Pressure) },
                dense = compact,
                modifier = cellModifier,
            )
        },
        { cellModifier ->
            FlowRateMetricCard(
                waterFlow = flow?.formatOneDecimal() ?: "—",
                cupFlow = output.cupFlow?.toDouble()?.formatOneDecimal() ?: "—",
                waterLabel = clearText(ClearText.WaterFlow),
                cupLabel = stringResource(DashboardRes.string.device_dashboard_cup_flow_label),
                onClick = { onToggle(FlowRate) },
                selected = state.visibleCharts.contains(FlowRate),
                dense = compact,
                modifier = cellModifier,
            )
        },
        { cellModifier ->
            MetricCard(
                label = stringResource(CoreRes.string.common_time),
                value = (liveSample?.key?.toDouble()
                    ?: state.brew.shotDurationSeconds.takeIf { state.brew.historyId != null }
                    ?: state.brew.time.coerceAtLeast(0).toDouble()).formatOneDecimal(),
                unit = stringResource(CoreRes.string.common_sec),
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
                selected = false,
                dense = compact,
                modifier = cellModifier,
            )
        },
        { cellModifier ->
            MetricCard(
                label = stringResource(DashboardRes.string.device_dashboard_output),
                value = output.value,
                unit = output.unit,
                footer = output.note,
                accent = BrewChartPalette.Weight,
                selected = false,
                dense = compact,
                modifier = cellModifier,
            )
        },
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(columns.coerceIn(1, TabletColumns)).forEach { row ->
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = if (compact) CompactMetricCardHeight else StandardMetricCardHeight)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cell -> cell(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun FlowRateMetricCard(
    waterFlow: String,
    cupFlow: String,
    waterLabel: String,
    cupLabel: String,
    selected: Boolean,
    dense: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) BrewChartPalette.WaterFlow.copy(alpha = MetricTintAlpha) else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(if (selected) BrewChartPalette.WaterFlow else BrewChartPalette.WaterFlow.copy(alpha = InactiveAccentAlpha)))
        Column(
            Modifier.weight(1f).padding(horizontal = 7.dp, vertical = if (dense) 3.dp else 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(stringResource(DashboardRes.string.dashboard_flow_title), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(waterLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = BrewChartPalette.WaterFlow, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(waterFlow, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                        Text(" ml/s", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                VerticalDivider(Modifier.height(30.dp).padding(horizontal = 2.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(cupLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = BrewChartPalette.CupFlow, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(cupFlow, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                        Text(" g/s", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

private data class OutputReading(val value: String, val unit: String, val note: String? = null, val cupFlow: Float? = null)

@Composable
private fun outputReading(
    state: DeviceDashboardViewState,
    profile: ProfileListViewState.Profile?,
    weight: Double?,
    volume: Double?,
    cupFlowGramsPerSecond: Float?,
    scaleWeightGrams: Float?,
    scaleConnected: Boolean,
    targetWeightGrams: Float?,
    nativeTargetWeightGrams: Float?,
): OutputReading {
    val history = state.readingsAreHistorical()
    val finish = profile?.finishCondition
    val historicalStopTarget = state.brew.historyStopWeightGrams
    val historicalRecipeTarget = if (state.brew.historyId != null) {
        historicalRecipeValues(
            state.brew.doseGrams,
            state.brew.recipeRatio,
            state.brew.recipeTargetGrams,
            (state.brew.phaseProgram as? BrewProgram.Phases)?.recipe?.settings,
        ).third?.toDouble()
    } else null
    val volumeFinish = targetWeightGrams == null && nativeTargetWeightGrams == null &&
        historicalStopTarget == null && finish is Condition.Volume
    val target = historicalRecipeTarget ?: targetWeightGrams ?: nativeTargetWeightGrams ?: historicalStopTarget?.toDouble() ?: when (finish) {
        is Condition.Weight -> finish.target
        is Condition.Volume -> finish.target
        null -> null
    }
    val connected = state.device.connectionStatus == DeviceDashboardViewState.Device.ConnectionStatus.Connected
    val liveScaleWeight = when {
        targetWeightGrams != null -> scaleWeightGrams?.toDouble()
        history && state.brew.historyFinalWeightGrams != null -> state.brew.historyFinalWeightGrams.toDouble()
        else -> weight
    }
    val canReadOutput = if (targetWeightGrams != null) {
        scaleConnected && liveScaleWeight != null
    } else if (volumeFinish) {
        history || connected
    } else if (history) {
        state.device.smartScaleConnected || (liveScaleWeight ?: 0.0) > 0.0
    } else {
        connected && (state.device.smartScaleConnected || scaleConnected)
    }
    val actual = when {
        !canReadOutput -> null
        volumeFinish -> volume
        else -> liveScaleWeight
    }?.formatOneDecimal() ?: "—"
    val value = target?.let { "$actual/${it.toDouble().formatOneDecimal()}" } ?: actual
    val unit = if (volumeFinish) CoreRes.string.unit_milliliters else CoreRes.string.unit_grams
    val note = when {
        !history && !connected -> clearText(ClearText.DeviceDisconnected)
        !canReadOutput && !scaleConnected -> clearText(ClearText.NoScale)
        else -> null
    }
    val cupFlow = cupFlowGramsPerSecond.takeIf {
        if (targetWeightGrams != null) scaleConnected
        else history || state.device.smartScaleConnected
    }
    return OutputReading(value, stringResource(unit), note, cupFlow)
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String = "",
    footer: String? = null,
    accent: Color,
    selected: Boolean,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (onClick != null) accent.copy(alpha = MetricTintAlpha) else MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(if (selected) accent else accent.copy(alpha = InactiveAccentAlpha)))
        Column(
            Modifier.weight(1f).padding(
                horizontal = if (dense) 8.dp else 10.dp,
                vertical = if (dense) 4.dp else 8.dp,
            )
        ) {
            Text(
                label,
                style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (onClick != null) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!footer.isNullOrBlank()) {
                Text(
                    footer,
                    style = if (dense) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (dense || '\n' in footer) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    value,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = if (dense) 20.sp else 32.sp,
                    lineHeight = if (dense) 24.sp else 36.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unit.isNotBlank()) {
                    Text(
                        unit,
                        modifier = Modifier.padding(start = 3.dp, bottom = if (dense) 2.dp else 4.dp),
                        style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun dashboardStatusText(state: DeviceDashboardViewState): String = when {
    state.device.brewStatus == DeviceDashboardViewState.Device.BrewStatus.Manual && state.manualCyclesExcludedFromCharts -> {
        if (state.brew.time > 0) clearText(ClearText.FlushLastShot) else clearText(ClearText.FlushChartsPaused)
    }
    state.brew.historyTarget != null || (!state.device.isBrewing && state.brew.time > 0) -> stringResource(
        DashboardRes.string.device_dashboard_last_shot
    )
    state.device.connectionStatus != DeviceDashboardViewState.Device.ConnectionStatus.Connected -> stringResource(
        DashboardRes.string.device_dashboard_disconnected
    )
    state.device.isBrewing -> stringResource(DashboardRes.string.device_dashboard_live)
    else -> stringResource(DashboardRes.string.device_dashboard_ready)
}

private fun DeviceDashboardViewState.readingsAreHistorical() =
    brew.historyTarget != null || (!device.isBrewing && brew.time > 0) ||
        (device.brewStatus == DeviceDashboardViewState.Device.BrewStatus.Manual && manualCyclesExcludedFromCharts)

private fun Double.formatOneDecimal() = (round(this * 10.0) / 10.0).toFloat().toString()

private const val MetricTintAlpha = 0.065f
private const val InactiveAccentAlpha = 0.35f

private const val PhoneColumns = 2
private const val TabletColumns = 4
private val PhoneMetricCardHeight = 68.dp
private val CompactMetricCardHeight = 68.dp
private val StandardMetricCardHeight = 92.dp
