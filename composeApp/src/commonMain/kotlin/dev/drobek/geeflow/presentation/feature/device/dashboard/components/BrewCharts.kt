package dev.drobek.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart.PersistentMarkerScope
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.AreaFill
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.LineFill
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.LineProvider
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Volume
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Weight
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import dev.drobek.geeflow.ui.isHeightCompact
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.disabled
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.device_dashboard_select_charts
import geeflow.composeapp.generated.resources.unit_bar
import geeflow.composeapp.generated.resources.unit_grams
import geeflow.composeapp.generated.resources.unit_grams_per_second
import geeflow.composeapp.generated.resources.unit_milliliters
import geeflow.composeapp.generated.resources.unit_milliliters_per_second
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private data class BrewChartSeries(
    val yValues: List<Number>,
    val color: Color,
    val unit: String
)

private data class TargetBrewData(
    val tX: List<Double>,
    val tY: List<Double>,
    val color: Color,
    val unit: String
)

@Composable
internal fun BrewCharts(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    selectedProfile: Profile? = null,
    modifier: Modifier = Modifier
) {
    val chartModifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
        .fillMaxWidth()
        .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)

    val sortedEntries = brew.data.entries.sortedBy { it.key }
    val xValues = sortedEntries.map { it.key.toDouble() }
    val sortedPoints = sortedEntries.map { it.value }

    val targetData = selectedProfile?.targetData ?: emptyMap()
    val targetPressure = targetData.map { it.key.toDouble() to it.value.pressure.toDouble() }.toMap()
    val targetFlow = targetData.map { it.key.toDouble() to it.value.volumePerSecond.toDouble() }.toMap()

    val maxBrewX = xValues.maxOrNull() ?: 0.0
    val maxX = maxOf(maxBrewX + 5f, 20.0)

    val syncState = rememberBrewSyncState()

    val showPressure = visibleCharts.contains(Pressure)
    val showFlowRate = visibleCharts.contains(FlowRate)
    val showWeightRate = visibleCharts.contains(WeightRate)
    val showVolume = visibleCharts.contains(Volume)
    val showWeight = visibleCharts.contains(Weight)

    val showFlowChart = showFlowRate || showWeightRate
    val showAccumulatedChart = showVolume || showWeight

    if (!showPressure && !showFlowChart && !showAccumulatedChart) {
        ChartsNotSelected(modifier = modifier.fillMaxSize().then(chartModifier))
        return
    }

    val errorColor = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val waterColor = GeeFlowTheme.colors.water
    val waterVariant = GeeFlowTheme.colors.waterVariant

    val sortedPressureTarget = targetPressure.entries.sortedBy { it.key }.takeIf { it.isNotEmpty() }
    val sortedFlowTarget = targetFlow.entries.sortedBy { it.key }.takeIf { it.isNotEmpty() }

    val unitBar = stringResource(Res.string.unit_bar)
    val unitGrams = stringResource(Res.string.unit_grams)
    val unitGramsPerSecond = stringResource(Res.string.unit_grams_per_second)
    val unitMl = stringResource(Res.string.unit_milliliters)
    val unitMlPerSecond = stringResource(Res.string.unit_milliliters_per_second)

    val pressureSeries = listOf(BrewChartSeries(sortedPoints.map { it.pressure }, errorColor, unitBar))
    val pressureTarget = sortedPressureTarget?.let {
        TargetBrewData(it.map { e -> e.key }, it.map { e -> e.value }, errorColor.copy(alpha = 0.5f), unitBar)
    }

    val flowSeries = buildList {
        if (showFlowRate) add(BrewChartSeries(sortedPoints.map { it.volumePerSecond }, waterColor, unitMlPerSecond))
        if (showWeightRate) add(BrewChartSeries(sortedPoints.map { it.weightPerSecond }, onSurface, unitGramsPerSecond))
    }
    val flowTarget = if (showFlowRate) {
        sortedFlowTarget?.let {
            TargetBrewData(it.map { e -> e.key }, it.map { e -> e.value }, waterColor.copy(alpha = 0.5f), unitMlPerSecond)
        }
    } else null

    val accumSeries = buildList {
        if (showVolume) add(BrewChartSeries(sortedPoints.map { it.volume }, waterVariant, unitMl))
        if (showWeight) add(BrewChartSeries(sortedPoints.map { it.weight }, onSurface, unitGrams))
    }

    if (isHeightCompact()) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rowModifier = Modifier.weight(1f).fillMaxSize().then(chartModifier)
            if (showPressure) BrewDataChart(
                xValues = xValues,
                chartSeries = pressureSeries,
                target = pressureTarget,
                yFloor = 12.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 0,
                modifier = rowModifier
            )
            if (showFlowChart) BrewDataChart(
                xValues = xValues,
                chartSeries = flowSeries,
                target = flowTarget,
                yFloor = 12.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 1,
                modifier = rowModifier
            )
            if (showAccumulatedChart) BrewDataChart(
                xValues = xValues,
                chartSeries = accumSeries,
                target = null,
                yFloor = 40.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 2,
                modifier = rowModifier
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val columnModifier = Modifier.weight(1f).fillMaxSize().then(chartModifier)
            if (showPressure) BrewDataChart(
                xValues = xValues,
                chartSeries = pressureSeries,
                target = pressureTarget,
                yFloor = 12.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 0,
                modifier = columnModifier
            )
            if (showFlowChart) BrewDataChart(
                xValues = xValues,
                chartSeries = flowSeries,
                target = flowTarget,
                yFloor = 12.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 1,
                modifier = columnModifier
            )
            if (showAccumulatedChart) BrewDataChart(
                xValues = xValues,
                chartSeries = accumSeries,
                target = null,
                yFloor = 40.0,
                maxX = maxX,
                syncState = syncState,
                chartId = 2,
                modifier = columnModifier
            )
        }
    }
}

@Composable
private fun ChartsNotSelected(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.device_dashboard_select_charts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BrewDataChart(
    xValues: List<Double>,
    chartSeries: List<BrewChartSeries>,
    target: TargetBrewData? = null,
    yFloor: Double,
    maxX: Double,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }

    LaunchedEffect(xValues, chartSeries, target) {
        producer.runTransaction {
            lineSeries {
                chartSeries.forEach { s ->
                    if (xValues.isNotEmpty()) series(x = xValues, y = s.yValues)
                    else series(x = listOf(0.0), y = listOf(0.0))
                }
                target?.let { t ->
                    if (t.tX.isNotEmpty()) series(x = t.tX, y = t.tY)
                    else series(x = listOf(0.0), y = listOf(0.0))
                }
            }
        }
    }

    val maxDataY = chartSeries.flatMap { it.yValues }.maxOfOrNull { it.toDouble() } ?: 0.0
    val maxTargetY = target?.tY?.maxOrNull() ?: 0.0
    val maxY = maxOf(yFloor, maxDataY, maxTargetY)

    val colors = chartSeries.map { it.color }
    val units = chartSeries.map { it.unit } + listOfNotNull(target?.unit)
    val marker = rememberBrewChartMarker(units)
    val listener = rememberBrewMarkerVisibilityListener(syncState, chartId)
    val syncMarkerX = if (syncState.activeChartId != chartId) syncState.markerX else null

    BrewChart(
        modelProducer = producer,
        colors = colors,
        targetColor = target?.color,
        maxX = maxX,
        maxY = maxY,
        marker = marker,
        markerVisibilityListener = listener,
        syncMarkerX = syncMarkerX,
        modifier = modifier,
    )
}

@Composable
private fun BrewChart(
    modelProducer: CartesianChartModelProducer,
    colors: List<Color>,
    targetColor: Color? = null,
    maxX: Double,
    maxY: Double,
    marker: CartesianMarker,
    markerVisibilityListener: CartesianMarkerVisibilityListener,
    syncMarkerX: Double?,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty() && targetColor == null) return

    val vicoScrollState = rememberVicoScrollState(scrollEnabled = false)
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = Zoom.Content,
        minZoom = Zoom.Content,
        maxZoom = Zoom.Content,
    )
    val persistentMarkers: (PersistentMarkerScope.(ExtraStore) -> Unit)? = syncMarkerX?.let { x -> { marker at x } }

    CartesianChartHost(
        zoomState = zoomState,
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineProvider.series(
                    *colors.map { color ->
                        LineCartesianLayer.rememberLine(
                            fill = LineFill.single(Fill(color)),
                            areaFill = AreaFill.single(Fill(Brush.verticalGradient(listOf(color.disabled(), Color.Transparent)))),
                        )
                    }.toTypedArray() + listOfNotNull(
                        targetColor?.let { color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineFill.single(Fill(color)),
                                stroke = LineCartesianLayer.LineStroke.Dashed(),
                                areaFill = null,
                            )
                        }
                    ).toTypedArray()
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = maxX, minY = 0.0, maxY = maxY)
            ),
            getXStep = { getXStep(maxX) },
            marker = marker,
            markerVisibilityListener = markerVisibilityListener,
            markerController = CartesianMarkerController.rememberShowOnPress(consumeMoveEvents = true),
            persistentMarkers = persistentMarkers,
            startAxis = VerticalAxis.rememberStart(
                tick = null,
                label = rememberAxisLabelComponent(
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                ),
                line = null,
                tickLength = 2.dp,
                itemPlacer = VerticalAxis.ItemPlacer.step(step = { 2.0 }),
                valueFormatter = { _, value, _ -> value.toLong().toString() },
                guideline = rememberAxisGuidelineComponent(
                    shape = DashedShape(
                        shape = CircleShape,
                        dashLength = 1.dp,
                        gapLength = 5.dp
                    )
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                tick = null,
                label = rememberAxisLabelComponent(
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                ),
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                    spacing = { 1 },
                    shiftExtremeLines = false,
                    addExtremeLabelPadding = false
                ),
                valueFormatter = { _, value, _ -> value.toLong().toString() },
                guideline = rememberAxisGuidelineComponent(
                    shape = DashedShape(shape = CircleShape, dashLength = 1.dp, gapLength = 5.dp)
                )
            ),
        ),
        modelProducer = modelProducer,
        animationSpec = null,
        animateIn = false,
        modifier = modifier,
        scrollState = vicoScrollState
    )
}

private fun getXStep(maxValue: Double, targetStepCount: Int = 10): Double {
    if (maxValue <= 0) return 1.0
    val roughStep = maxValue / targetStepCount
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val normalizedStep = roughStep / magnitude

    val niceMultiplier = when {
        normalizedStep <= 1.5 -> 1.0
        normalizedStep <= 3.0 -> 2.0
        normalizedStep <= 7.0 -> 5.0
        else -> 10.0
    }

    return niceMultiplier * magnitude
}
