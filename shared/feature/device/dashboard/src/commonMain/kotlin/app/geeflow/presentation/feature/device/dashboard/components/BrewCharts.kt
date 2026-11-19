package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.data.user.model.FlowChartRange
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Volume
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Weight
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate
import app.geeflow.presentation.feature.device.dashboard.model.hasRecordedNativeCupFlow
import app.geeflow.presentation.feature.device.dashboard.model.BrewChartModel
import app.geeflow.presentation.feature.device.dashboard.model.ChartPhaseBoundary
import app.geeflow.presentation.feature.device.dashboard.model.liveChartTimeHorizon
import app.geeflow.presentation.feature.device.dashboard.model.oneSecondScaleFlowSeries
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.disabled
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.AreaFill
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.LineFill
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.LineProvider
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_grams_per_second
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_actual
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_cup_flow_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_flow_range_exceeded
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_pressure_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_phase_planned
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_phase_reached
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_select_charts
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_volume_relative_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_weight_relative_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_water_flow_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_bookoo_reported_flow
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_scale_smoothed_flow
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_bookoo_weight
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private data class BrewChartSeries(
    val yValues: List<Number>,
    val color: Color,
    val unit: String,
    val markerYValues: List<Number> = yValues,
    val emphasized: Boolean = false,
    val label: String? = null,
    val xValues: List<Double>? = null,
    val decimalValues: Boolean = false,
)

private data class TargetBrewData(
    val tX: List<Double>,
    val tY: List<Double>,
    val color: Color,
    val unit: String,
    val markerYValues: List<Double> = tY,
)

private data class CombinedBrewChartSeries(
    val label: String,
    val actual: BrewChartSeries,
    val target: TargetBrewData?,
)

@Composable
internal fun BrewCharts(
    model: BrewChartModel,
    visibleCharts: Set<DashboardChartType>,
    modifier: Modifier = Modifier,
    combinedOnPhone: Boolean = true,
    scrollPanels: Boolean = true,
    forceCombined: Boolean? = null,
) {
    BoxWithConstraints(modifier) {
        val compact = maxWidth < 600.dp
        BrewChartsContent(
            model = model,
            visibleCharts = visibleCharts,
            combined = forceCombined ?: (combinedOnPhone && compact),
            scrollPanels = scrollPanels,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BrewChartsContent(
    model: BrewChartModel,
    visibleCharts: Set<DashboardChartType>,
    combined: Boolean,
    scrollPanels: Boolean,
) {
    // A shared viewport keeps the tablet's vertically stacked metric panels on the same time window.
    val scrollState = rememberVicoScrollState(scrollEnabled = true)
    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = Zoom.Content,
        minZoom = Zoom.Content,
        maxZoom = Zoom.max(Zoom.fixed(4f), Zoom.Content),
    )
    val chartFlowRange = FlowChartRange.TWELVE
    val measurements = model.measurements.entries.sortedBy { it.key }
    val xValues = measurements.map { it.key.toDouble() }
    val points = measurements.map { it.value }
    val nativeCupFlow = if (hasRecordedNativeCupFlow(points)) points.map { it.weightPerSecond } else emptyList()
    val scaleMeasurements = model.scaleMeasurements.filter { it.decoded }.sortedBy { it.elapsedMillis }
    val scaleXValues = scaleMeasurements.map { it.elapsedMillis / 1000.0 }
    val targetEntries = model.targets.entries.sortedBy { it.key }
    val boundaries = model.boundaries
    val measuredEnd = maxOf(xValues.maxOrNull() ?: 0.0, scaleXValues.maxOrNull() ?: 0.0)
    val maxX = if (model.liveTimeline) {
        liveChartTimeHorizon(measuredEnd)
    } else if (model.fitToMeasurements) {
        (model.shotEndSeconds?.takeIf { it > 0.0 } ?: measuredEnd).coerceAtLeast(0.001)
    } else {
        maxOf(
            xValues.maxOrNull() ?: 0.0,
            scaleXValues.maxOrNull() ?: 0.0,
            targetEntries.maxOfOrNull { it.key.toDouble() } ?: 0.0,
            boundaries.maxOfOrNull { it.seconds } ?: 0.0,
            MinChartX,
        ) + ChartXPadding
    }
    val syncState = rememberBrewSyncState()
    val pressureColor = BrewChartPalette.Pressure
    val waterColor = BrewChartPalette.WaterFlow
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cupFlowColor = BrewChartPalette.CupFlow
    val reportedFlowColor = BrewChartPalette.CupFlow.copy(alpha = 0.9f)
    val derivedFlowColor = BrewChartPalette.CupFlow.copy(alpha = 0.65f)
    val bookooWeightColor = BrewChartPalette.Weight
    val volumeColor = if (isDarkSurface) Color(0xFFFF9AAE) else Color(0xFFB83F58)

    val showPressure = Pressure in visibleCharts
    val showFlow = FlowRate in visibleCharts
    val showCupFlow = WeightRate in visibleCharts
    val showVolume = Volume in visibleCharts
    val showWeight = Weight in visibleCharts
    val chartCount = listOf(showPressure, showFlow, showCupFlow, showVolume, showWeight).count { it }
    if (chartCount == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.device_dashboard_select_charts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val unitBar = stringResource(CoreRes.string.unit_bar)
    val unitMlPerSecond = stringResource(CoreRes.string.unit_milliliters_per_second)
    val unitGramsPerSecond = stringResource(CoreRes.string.unit_grams_per_second)
    val unitMl = stringResource(CoreRes.string.unit_milliliters)
    val unitGrams = stringResource(CoreRes.string.unit_grams)
    val bookooReportedLabel = stringResource(Res.string.device_dashboard_bookoo_reported_flow)
    val bookooDerivedLabel = stringResource(Res.string.device_dashboard_scale_smoothed_flow)
    val bookooWeightLabel = stringResource(Res.string.device_dashboard_bookoo_weight)
    val pressureTarget = targetEntries.takeIf { entries -> entries.any { it.value.pressure > 0f } }?.let { entries ->
        TargetBrewData(
            tX = entries.map { it.key.toDouble() },
            tY = entries.map { it.value.pressure.toDouble() },
            color = pressureColor,
            unit = unitBar,
        )
    }
    val flowTarget = targetEntries.takeIf { entries -> entries.any { it.value.volumePerSecond > 0f } }?.let { entries ->
        TargetBrewData(
            tX = entries.map { it.key.toDouble() },
            tY = entries.map { it.value.volumePerSecond.toDouble() },
            color = waterColor,
            unit = unitMlPerSecond,
        )
    }
    val maxFlow = maxOf(
        points.maxOfOrNull { it.volumePerSecond.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.volumePerSecond.toDouble() } ?: 0.0,
    )
    val flowOverflow = if (maxFlow > chartFlowRange.maxMillilitersPerSecond) {
        stringResource(
            Res.string.device_dashboard_flow_range_exceeded,
            maxFlow.formatOneDecimal(),
            chartFlowRange.maxMillilitersPerSecond,
        )
    } else {
        null
    }

    val cupTarget = targetEntries.takeIf { entries -> entries.any { it.value.weightPerSecond > 0f } }?.let { entries ->
        TargetBrewData(
            entries.map { it.key.toDouble() },
            entries.map { it.value.weightPerSecond.toDouble() },
            cupFlowColor,
            unitGramsPerSecond,
        )
    }
    val volumeTarget = targetEntries.takeIf { entries -> entries.any { it.value.volume > 0f } }?.let { entries ->
        TargetBrewData(
            entries.map { it.key.toDouble() },
            entries.map { it.value.volume.toDouble() },
            volumeColor,
            unitMl,
        )
    }
    val weightTarget = targetEntries.takeIf { entries -> entries.any { it.value.weight > 0f } }?.let { entries ->
        TargetBrewData(
            entries.map { it.key.toDouble() },
            entries.map { it.value.weight.toDouble() },
            neutralColor,
            unitGrams,
        )
    }

    // Series are normalized to a shared 0–12 scale; markers retain the raw values and units.
    fun relativeValue(value: Number, maximum: Double) = value.toDouble() / maximum * UnifiedChartMax
    fun relativeTarget(target: TargetBrewData?, maximum: Double) = target?.copy(
        tY = target.tY.map { relativeValue(it, maximum) },
    )
    fun toCombinedSeries(
        label: String,
        values: List<Number>,
        color: Color,
        unit: String,
        maximum: Double,
        target: TargetBrewData?,
        emphasized: Boolean = false,
    ) = CombinedBrewChartSeries(
        label = label,
        actual = BrewChartSeries(
            yValues = values.map { relativeValue(it, maximum) },
            color = color,
            unit = unit,
            markerYValues = values,
            emphasized = emphasized,
            label = label,
        ),
        target = relativeTarget(target, maximum)?.copy(color = color),
    )

    val pressureMaximum = UnifiedChartMax
    val combinedFlowOverflow = if (maxFlow > UnifiedChartMax) {
        stringResource(
            Res.string.device_dashboard_flow_range_exceeded,
            maxFlow.formatOneDecimal(),
            UnifiedChartMax.toInt(),
        )
    } else {
        null
    }
    val reportedScaleFlows = scaleMeasurements.mapNotNull { sample ->
        sample.reportedFlowGramsPerSecond?.let { sample to it }
    }
    val derivedScaleFlows = oneSecondScaleFlowSeries(scaleMeasurements)
    val bookooWeights = scaleMeasurements.mapNotNull { sample -> sample.weightGrams?.let { sample to it } }
    val reportedFlowSeries = BrewChartSeries(
        yValues = reportedScaleFlows.map { it.second },
        color = reportedFlowColor,
        unit = unitGramsPerSecond,
        markerYValues = reportedScaleFlows.map { it.second },
        label = bookooReportedLabel,
        xValues = reportedScaleFlows.map { it.first.elapsedMillis / 1000.0 },
        decimalValues = true,
    )
    val derivedFlowSeries = BrewChartSeries(
        yValues = derivedScaleFlows.map { it.second },
        color = derivedFlowColor,
        unit = unitGramsPerSecond,
        markerYValues = derivedScaleFlows.map { it.second },
        label = bookooDerivedLabel,
        xValues = derivedScaleFlows.map { it.first.elapsedMillis / 1000.0 },
        decimalValues = true,
    )
    val bookooWeightSeries = BrewChartSeries(
        yValues = bookooWeights.map { it.second },
        color = bookooWeightColor,
        unit = unitGrams,
        markerYValues = bookooWeights.map { it.second },
        label = bookooWeightLabel,
        xValues = bookooWeights.map { it.first.elapsedMillis / 1000.0 },
        decimalValues = true,
    )
    val flowAxisMaximum = maxOf(
        FlowChartRange.FOUR.maxMillilitersPerSecond.toDouble(),
        points.maxOfOrNull { it.volumePerSecond.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.volumePerSecond.toDouble() } ?: 0.0,
        points.maxOfOrNull { it.weightPerSecond.toDouble() } ?: 0.0,
        reportedScaleFlows.maxOfOrNull { it.second.toDouble() } ?: 0.0,
        derivedScaleFlows.maxOfOrNull { it.second.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.weightPerSecond.toDouble() } ?: 0.0,
    )
    val cupFlowMaximum = maxOf(
        12.0,
        points.maxOfOrNull { it.weightPerSecond.toDouble() } ?: 0.0,
        reportedScaleFlows.maxOfOrNull { it.second.toDouble() } ?: 0.0,
        derivedScaleFlows.maxOfOrNull { it.second.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.weightPerSecond.toDouble() } ?: 0.0,
    )
    val volumeMaximum = maxOf(
        40.0,
        points.maxOfOrNull { it.volume.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.volume.toDouble() } ?: 0.0,
    )
    val weightMaximum = maxOf(
        40.0,
        points.maxOfOrNull { it.weight.toDouble() } ?: 0.0,
        bookooWeights.maxOfOrNull { it.second.toDouble() } ?: 0.0,
        targetEntries.maxOfOrNull { it.value.weight.toDouble() } ?: 0.0,
    )
    val pressureTitle = stringResource(Res.string.device_dashboard_pressure_chart)
    val flowTitle = stringResource(Res.string.device_dashboard_water_flow_chart)
    val cupFlowTitle = stringResource(Res.string.device_dashboard_cup_flow_chart)
    val relativeVolumeTitle = stringResource(Res.string.device_dashboard_volume_relative_chart)
    val relativeWeightTitle = stringResource(Res.string.device_dashboard_weight_relative_chart)
    val weightColor = neutralColor
    val combinedMetrics = buildList<CombinedBrewChartSeries> {
        if (showPressure) add(
            toCombinedSeries(
                pressureTitle,
                points.map { it.pressure }, pressureColor, unitBar, pressureMaximum, pressureTarget,
                emphasized = true,
            ),
        )
        if (showFlow) add(
            toCombinedSeries(
                flowTitle,
                points.map { it.volumePerSecond }, waterColor, unitMlPerSecond, flowAxisMaximum, flowTarget,
                emphasized = true,
            ),
        )
        if (showCupFlow) add(
            toCombinedSeries(
                cupFlowTitle,
                nativeCupFlow, cupFlowColor, unitGramsPerSecond, flowAxisMaximum, cupTarget,
            ),
        )
        if (showCupFlow && reportedFlowSeries.yValues.isNotEmpty()) add(
            CombinedBrewChartSeries(
                bookooReportedLabel,
                reportedFlowSeries.copy(yValues = reportedFlowSeries.yValues.map { relativeValue(it, flowAxisMaximum) }),
                null,
            ),
        )
        if (showCupFlow && derivedFlowSeries.yValues.isNotEmpty()) add(
            CombinedBrewChartSeries(
                bookooDerivedLabel,
                derivedFlowSeries.copy(yValues = derivedFlowSeries.yValues.map { relativeValue(it, flowAxisMaximum) }),
                null,
            ),
        )
        if (showVolume) add(
            toCombinedSeries(
                relativeVolumeTitle,
                points.map { it.volume }, volumeColor, unitMl, volumeMaximum, volumeTarget,
            ),
        )
        if (showWeight) add(
            toCombinedSeries(
                relativeWeightTitle,
                points.map { it.weight }, weightColor, unitGrams, weightMaximum, weightTarget,
            ),
        )
        if (showWeight && bookooWeightSeries.yValues.isNotEmpty()) add(
            CombinedBrewChartSeries(
                bookooWeightLabel,
                bookooWeightSeries.copy(yValues = bookooWeightSeries.yValues.map { relativeValue(it, weightMaximum) }),
                null,
            ),
        )
    }

    fun CombinedBrewChartSeries.legendLabel(): String {
        val cleanLabel = label
            .removeSuffix(" (relative)")
            .removeSuffix("（相对）")
        val unitSuffix = "(${actual.unit})"
        return if (cleanLabel.endsWith(unitSuffix, ignoreCase = true)) {
            cleanLabel
        } else {
            "$cleanLabel $unitSuffix"
        }
    }

    @Composable
    fun panel(
        id: Int,
        actual: BrewChartSeries,
        target: TargetBrewData?,
        yMax: Double,
        yStep: Double,
        overflow: String? = null,
        additionalActual: List<BrewChartSeries> = emptyList(),
        modifier: Modifier,
    ) {
        ChartPanel(
            overflow = overflow,
            modifier = modifier,
        ) { chartModifier ->
            BrewDataChart(
                    xValues = xValues,
                    chartSeries = (listOf(actual) + additionalActual).filter { it.yValues.isNotEmpty() },
                    targets = listOfNotNull(target),
                    fixedMaxY = yMax,
                    yStep = yStep,
                    maxX = maxX,
                    phaseBoundaries = boundaries,
                    syncState = syncState,
                    chartId = id,
                    modifier = chartModifier,
                    zoomEnabled = true,
                    scrollState = scrollState,
                    zoomState = zoomState,
                    showTimeAxis = id == listOfNotNull(
                        0.takeIf { showPressure },
                        1.takeIf { showFlow },
                        2.takeIf { showCupFlow },
                        3.takeIf { showVolume },
                        4.takeIf { showWeight }
                    ).first(),
                    showPhaseLabels = id == listOfNotNull(
                        0.takeIf { showPressure },
                        1.takeIf { showFlow },
                        2.takeIf { showCupFlow },
                        3.takeIf { showVolume },
                        4.takeIf { showWeight }
                    ).first(),
                    pressureGuidelines = id == 0,
                )
        }
    }

    val chartContent: @Composable (Modifier) -> Unit = { itemModifier ->
        Column(
            modifier = if (chartCount > 2 && scrollPanels) itemModifier.verticalScroll(rememberScrollState()) else itemModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val panelModifier = if (chartCount > 2 && scrollPanels) Modifier.height(180.dp).fillMaxWidth() else Modifier.weight(1f).fillMaxWidth()
            if (showPressure) {
                panel(
                    id = 0,
                    actual = BrewChartSeries(points.map { it.pressure }, pressureColor, unitBar, emphasized = true),
                    target = pressureTarget,
                    yMax = 12.0,
                    yStep = 2.0,
                    modifier = panelModifier,
                )
            }
            if (showFlow) {
                panel(
                    id = 1,
                    actual = BrewChartSeries(points.map { it.volumePerSecond }, waterColor, unitMlPerSecond, emphasized = true, decimalValues = true),
                    target = flowTarget,
                    yMax = chartFlowRange.maxMillilitersPerSecond.toDouble(),
                    yStep = chartFlowRange.maxMillilitersPerSecond.toDouble() / 8.0,
                    overflow = flowOverflow,
                    modifier = panelModifier,
                )
            }
            if (showCupFlow) {
                panel(
                    id = 2,
                    actual = BrewChartSeries(nativeCupFlow, cupFlowColor, unitGramsPerSecond, decimalValues = true),
                    target = cupTarget,
                    yMax = cupFlowMaximum,
                    yStep = 2.0,
                    additionalActual = listOf(reportedFlowSeries, derivedFlowSeries).filter { it.yValues.isNotEmpty() },
                    modifier = panelModifier,
                )
            }
            if (showVolume) {
                panel(
                    id = 3,
                    actual = BrewChartSeries(points.map { it.volume }, volumeColor, unitMl),
                    target = volumeTarget,
                    yMax = maxOf(
                        40.0,
                        points.maxOfOrNull { it.volume.toDouble() } ?: 0.0,
                        targetEntries.maxOfOrNull { it.value.volume.toDouble() } ?: 0.0
                    ),
                    yStep = 10.0,
                    modifier = panelModifier,
                )
            }
            if (showWeight) {
                panel(
                    id = 4,
                    actual = BrewChartSeries(points.map { it.weight }, neutralColor, unitGrams),
                    target = weightTarget,
                    yMax = weightMaximum,
                    yStep = 10.0,
                    additionalActual = listOf(bookooWeightSeries).filter { it.yValues.isNotEmpty() },
                    modifier = panelModifier,
                )
            }
        }
    }

    @Composable
    fun combinedChart(modifier: Modifier) {
        ChartPanel(
            overflow = combinedFlowOverflow,
            modifier = modifier,
        ) { chartModifier ->
            BrewDataChart(
                xValues = xValues,
                chartSeries = combinedMetrics.map { it.actual }.filter { it.yValues.isNotEmpty() },
                targets = combinedMetrics.mapNotNull { it.target },
                fixedMaxY = UnifiedChartMax,
                yStep = 3.0,
                maxX = maxX,
                phaseBoundaries = boundaries,
                syncState = syncState,
                chartId = CombinedPhoneChartId,
                modifier = chartModifier,
                zoomEnabled = true,
                scrollState = scrollState,
                zoomState = zoomState,
                showAreas = false,
                flowAxisMaximum = flowAxisMaximum,
            )
        }
    }

    @Composable
    fun sharedLegend() {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            combinedMetrics.forEach { series ->
                val actualTimes = series.actual.xValues ?: xValues
                if (actualTimes.size > 1 && series.actual.yValues.size > 1) {
                    LegendItem("${series.legendLabel().replace(" (${series.actual.unit})", "")} · ${stringResource(Res.string.device_dashboard_actual)}", series.actual.color, false)
                }
                series.target?.let { target ->
                    LegendItem("${series.legendLabel().replace(" (${series.actual.unit})", "")} · ${clearText(ClearText.Target)}", target.color, true)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        sharedLegend()
        if (combined) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ml/s · g/s", style = MaterialTheme.typography.labelSmall, color = BrewChartPalette.WaterFlow)
                Text("bar", style = MaterialTheme.typography.labelSmall, color = BrewChartPalette.Pressure)
            }
            combinedChart(Modifier.weight(1f).fillMaxWidth())
        } else {
            chartContent(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun ChartPanel(
    modifier: Modifier = Modifier,
    overflow: String? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Box(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface).padding(6.dp)) {
        Column(Modifier.fillMaxSize()) {
            if (overflow != null) {
                Text(
                    overflow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                content(Modifier.fillMaxSize().clipToBounds())
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, dashed: Boolean, vertical: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.width(22.dp).height(8.dp)) {
            val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
            if (vertical) {
                drawLine(
                    color,
                    Offset(size.width / 2, 0f),
                    Offset(size.width / 2, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = effect,
                )
            } else {
                drawLine(
                    color,
                    Offset.Zero.copy(y = size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = effect,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PhaseBoundaryLegend(boundaries: List<ChartPhaseBoundary>) {
    val colors = MaterialTheme.colorScheme
    if (boundaries.any { it.actual }) {
        LegendItem(
            stringResource(Res.string.device_dashboard_phase_reached),
            colors.onSurfaceVariant.copy(alpha = ActualBoundaryAlpha),
            dashed = false,
            vertical = true,
        )
    }
    if (boundaries.any { !it.actual }) {
        LegendItem(
            stringResource(Res.string.device_dashboard_phase_planned),
            colors.outlineVariant,
            dashed = true,
            vertical = true,
        )
    }
}

@Composable
private fun BrewDataChart(
    xValues: List<Double>,
    chartSeries: List<BrewChartSeries>,
    targets: List<TargetBrewData>,
    fixedMaxY: Double,
    yStep: Double,
    maxX: Double,
    phaseBoundaries: List<ChartPhaseBoundary>,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier,
    zoomEnabled: Boolean = false,
    scrollState: VicoScrollState,
    zoomState: VicoZoomState,
    showAreas: Boolean = true,
    showTimeAxis: Boolean = true,
    showPhaseLabels: Boolean = true,
    flowAxisMaximum: Double? = null,
    pressureGuidelines: Boolean = false,
) {
    if (chartSeries.isEmpty() && targets.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, chartSeries, targets) {
        producer.runTransaction {
            lineModel {
                chartSeries.forEach { series ->
                    val seriesX = series.xValues ?: xValues
                    if (seriesX.isNotEmpty()) {
                        series(x = seriesX, y = series.yValues)
                    } else {
                        series(x = listOf(0.0), y = listOf(0.0))
                    }
                }
                targets.forEach { target ->
                    if (target.tX.isNotEmpty()) {
                        series(x = target.tX, y = target.tY)
                    } else {
                        series(x = listOf(0.0), y = listOf(0.0))
                    }
                }
            }
        }
    }
    val colors = chartSeries.map { it.color }
    val markerSeries = remember(xValues, chartSeries, targets) {
        chartSeries.map { series ->
            BrewMarkerSeries(
                series.xValues ?: xValues,
                series.yValues.map { it.toDouble() },
                series.color,
                series.unit,
                series.markerYValues.map { it.toDouble() },
            )
        } +
            targets.map { target -> BrewMarkerSeries(target.tX, target.tY, target.color, target.unit, target.markerYValues) }
    }
    val marker = rememberBrewChartMarker(markerSeries)
    val listener = rememberBrewMarkerVisibilityListener(syncState, chartId)
    val syncMarkerX = if (syncState.activeChartId != chartId) syncState.markerX else null
    BrewChart(
        modelProducer = producer,
        colors = colors,
        actualLineThicknesses = chartSeries.map { if (it.emphasized) PrimaryLineThickness else SecondaryLineThickness },
        targetColors = targets.map { it.color },
        maxX = maxX,
        maxY = fixedMaxY,
        yStep = yStep,
        marker = marker,
        markerVisibilityListener = listener,
        syncMarkerX = syncMarkerX,
        phaseBoundaries = phaseBoundaries,
        zoomEnabled = zoomEnabled,
        scrollState = scrollState,
        zoomState = zoomState,
        showAreas = showAreas,
        showTimeAxis = showTimeAxis,
        showPhaseLabels = showPhaseLabels,
        flowAxisMaximum = flowAxisMaximum,
        pressureGuidelines = pressureGuidelines,
        decimalYAxis = chartSeries.any { it.decimalValues },
        modifier = modifier,
    )
}

@Composable
private fun BrewChart(
    modelProducer: CartesianChartModelProducer,
    colors: List<Color>,
    actualLineThicknesses: List<Float>,
    targetColors: List<Color>,
    maxX: Double,
    maxY: Double,
    yStep: Double,
    marker: BrewChartMarker,
    markerVisibilityListener: CartesianMarkerVisibilityListener,
    syncMarkerX: Double?,
    phaseBoundaries: List<ChartPhaseBoundary>,
    modifier: Modifier = Modifier,
    zoomEnabled: Boolean = false,
    scrollState: VicoScrollState,
    zoomState: VicoZoomState,
    showAreas: Boolean = true,
    showTimeAxis: Boolean = true,
    showPhaseLabels: Boolean = true,
    flowAxisMaximum: Double? = null,
    pressureGuidelines: Boolean = false,
    decimalYAxis: Boolean = false,
) {
    BoxWithConstraints(modifier) {
        val tickCount = if (maxWidth < 500.dp) PhoneTimeDivisions else TabletTimeDivisions
        if (colors.isEmpty() && targetColors.isEmpty()) return@BoxWithConstraints
        val scope = rememberCoroutineScope()
        val phaseDecoration = brewPhaseDecoration(phaseBoundaries, showPhaseLabels)
        val syncDecoration = rememberBrewSyncDecoration(marker, syncMarkerX)
        Box(Modifier.fillMaxSize()) {
            CartesianChartHost(
            zoomState = zoomState,
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineProvider.series(
                        *colors.mapIndexed { index, color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineFill.single(Fill(color)),
                                stroke = LineCartesianLayer.LineStroke.Continuous(
                                    thickness = actualLineThicknesses.getOrElse(index) { SecondaryLineThickness }.dp,
                                ),
                                interpolator = LineCartesianLayer.Interpolator.cubic(),
                                areaFill = if (showAreas) AreaFill.single(
                                    Fill(Brush.verticalGradient(listOf(color.disabled(ChartAreaAlpha), Color.Transparent)))
                                ) else null,
                            )
                        }.toTypedArray() + targetColors.map { color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineFill.single(Fill(color.copy(alpha = TargetLineAlpha))),
                                stroke = LineCartesianLayer.LineStroke.Dashed(
                                    thickness = TargetLineThickness.dp,
                                    dashLength = 4.dp,
                                    gapLength = 4.dp,
                                ),
                                areaFill = null
                            )
                        }.toTypedArray(),
                    ),
                    rangeProvider = CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = maxX, minY = 0.0, maxY = maxY),
                ),
                getXStep = { _, _, _ -> getXStep(maxX, tickCount) },
                marker = marker,
                markerVisibilityListener = markerVisibilityListener,
                markerController = CartesianMarkerController.rememberShowOnPress(consumeMoveEvents = true),
                decorations = listOf(phaseDecoration, syncDecoration),
                startAxis = VerticalAxis.rememberStart(
                    size = BaseAxis.Size.Fixed(32.dp),
                    tick = null,
                    label = rememberAxisLabelComponent(
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = if (flowAxisMaximum != null) BrewChartPalette.WaterFlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ),
                    line = null,
                    tickLength = 2.dp,
                    itemPlacer = VerticalAxis.ItemPlacer.step(step = {
                        when {
                            flowAxisMaximum != null -> 3.0
                            pressureGuidelines -> 1.0
                            else -> yStep
                        }
                    }),
                    valueFormatter = { _, value, _ ->
                        if (pressureGuidelines && value % 2.0 != 0.0) HiddenAxisLabel
                        else if (flowAxisMaximum != null) (value / UnifiedChartMax * flowAxisMaximum).formatAxisValue()
                        else if (decimalYAxis) value.formatOneDecimal()
                        else if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                    },
                    guideline = rememberAxisGuidelineComponent(
                        fill = Fill(MaterialTheme.colorScheme.onSurface.copy(alpha = if (pressureGuidelines) 0.08f else 0.14f)),
                        shape = if (pressureGuidelines) RectangleShape
                        else DashedShape(CircleShape, dashLength = 2.dp, gapLength = 3.dp),
                    ),
                ),
                endAxis = flowAxisMaximum?.let {
                    VerticalAxis.rememberEnd(
                        size = BaseAxis.Size.Fixed(34.dp),
                        tick = null,
                        label = rememberAxisLabelComponent(
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = BrewChartPalette.Pressure,
                            ),
                        ),
                        line = null,
                        tickLength = 2.dp,
                        itemPlacer = VerticalAxis.ItemPlacer.step(step = { 1.0 }),
                        valueFormatter = { _, value, _ ->
                            if (value % 3.0 != 0.0) HiddenAxisLabel
                            else if (value % 1.0 == 0.0) value.toLong().toString() else value.formatOneDecimal()
                        },
                        guideline = rememberAxisGuidelineComponent(
                            fill = Fill(MaterialTheme.colorScheme.onSurface.copy(alpha = PressureGuidelineAlpha)),
                            shape = RectangleShape,
                        ),
                    )
                },
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = null,
                    tick = null,
                    label = if (showTimeAxis) {
                        rememberAxisLabelComponent(
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        null
                    },
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                        spacing = { 1 },
                        shiftExtremeLines = true,
                        addExtremeLabelPadding = true
                    ),
                    valueFormatter = { _, value, _ ->
                        val seconds = if (value % 1.0 == 0.0) value.toLong().toString() else value.formatOneDecimal()
                        "${seconds}s"
                    },
                    guideline = rememberAxisGuidelineComponent(
                        fill = Fill(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                        shape = DashedShape(CircleShape, dashLength = 2.dp, gapLength = 3.dp),
                    ),
                ),
            ),
            modelProducer = modelProducer,
            animationSpec = null,
            animateIn = false,
            // Keep the time labels clear of the fixed controls at the bottom of the brew screen.
            modifier = Modifier.fillMaxSize().padding(bottom = TimeAxisBottomClearance),
            scrollState = scrollState,
        )
            if (zoomEnabled) {
                TextButton(
                    onClick = {
                        scope.launch {
                            zoomState.animateZoom(Zoom.Content)
                            scrollState.scroll(Scroll.Absolute.Start)
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp),
                ) { Text("Fit") }
            }
        }
}
}

private const val ChartXPadding = 5.0
private const val MinChartX = 20.0
private const val StepNiceLow = 1.5
private const val StepNiceMid = 3.0
private const val StepNiceHigh = 7.0
private const val StepNice5 = 5.0
private const val StepNiceMax = 10.0

private fun getXStep(maxValue: Double, targetStepCount: Int = 10): Double {
    if (maxValue <= 0) return 1.0
    val roughStep = maxValue / targetStepCount
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val normalizedStep = roughStep / magnitude
    val niceMultiplier = when {
        normalizedStep <= StepNiceLow -> 1.0
        normalizedStep <= StepNiceMid -> 2.0
        normalizedStep <= StepNiceHigh -> StepNice5
        else -> StepNiceMax
    }
    return niceMultiplier * magnitude
}

private fun Double.formatOneDecimal() = ((this * DecimalPrecision).toInt() / DecimalPrecision).toString()

private fun Double.formatAxisValue() =
    if (this % 1.0 == 0.0) toLong().toString() else formatOneDecimal()

private const val TargetLineAlpha = 0.34f
private const val PressureGuidelineAlpha = 0.08f
private const val HiddenAxisLabel = "\u200B"
private const val TargetLineThickness = 1f
private const val SecondaryLineThickness = 1.1f
private const val PrimaryLineThickness = 2.2f
private const val DecimalPrecision = 10.0

private const val ChartAreaAlpha = 0.18f
private const val ActualBoundaryAlpha = 0.72f
private const val UnifiedChartMax = 12.0
private const val CombinedPhoneChartId = 5

private const val PhoneTimeDivisions = 4
private const val TabletTimeDivisions = 10
private val TimeAxisBottomClearance = 18.dp
