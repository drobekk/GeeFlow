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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
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
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.FlowRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Pressure
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Volume
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Weight
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.WeightRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import dev.drobek.geeflow.ui.isHeightCompact
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.disabled
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.device_dashboard_select_charts
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewCharts(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    selectedProfile: Profile? = null,
    modifier: Modifier = Modifier
) {
    BrewChartsSection(
        brew = brew,
        visibleCharts = visibleCharts,
        selectedProfile = selectedProfile,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
internal fun BrewChartsSection(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    selectedProfile: Profile? = null,
    modifier: Modifier = Modifier
) {
    val chartModifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
        .fillMaxWidth()
        .padding(16.dp)

    val sortedEntries = brew.data.entries.sortedBy { it.key }
    val xValues = sortedEntries.map { it.key.toDouble() }
    val sortedPoints = sortedEntries.map { it.value }

    val targetData = selectedProfile?.targetData ?: emptyMap()
    val targetPressure = targetData.map { it.key.toDouble() to it.value.pressure.toDouble() }.toMap()
    val targetFlow = targetData.map { it.key.toDouble() to it.value.volumePerSecond.toDouble() }.toMap()

    val maxBrewX = xValues.maxOrNull() ?: 0.0
    val maxTargetPressureX = targetPressure.keys.maxOrNull() ?: 0.0
    val maxTargetFlowX = targetFlow.keys.maxOrNull() ?: 0.0
    val maxX = maxOf(maxBrewX, maxTargetPressureX, maxTargetFlowX, 20.0)

    val syncState = rememberBrewSyncState()

    val showPressure = visibleCharts.contains(Pressure)
    val showFlowRate = visibleCharts.contains(FlowRate)
    val showWeightRate = visibleCharts.contains(WeightRate)
    val showVolume = visibleCharts.contains(Volume)
    val showWeight = visibleCharts.contains(Weight)

    val showFlowChart = showFlowRate || showWeightRate
    val showAccumulatedChart = showVolume || showWeight

    if (!showPressure && !showFlowChart && !showAccumulatedChart) {
        ChartsNotSelected(modifier = modifier.then(chartModifier))
        return
    }

    val pressureChart: @Composable () -> Unit = {
        PressureChart(
            xValues = xValues,
            sortedPoints = sortedPoints,
            targetSeries = targetPressure.ifEmpty { null },
            maxX = maxX,
            syncState = syncState,
            chartId = 0,
            modifier = Modifier.fillMaxSize().then(chartModifier)
        )
    }

    val flowRateChart: @Composable () -> Unit = {
        FlowRateChart(
            xValues = xValues,
            sortedPoints = sortedPoints,
            targetSeries = targetFlow.ifEmpty { null },
            showFlowRate = showFlowRate,
            showWeightRate = showWeightRate,
            maxX = maxX,
            syncState = syncState,
            chartId = 1,
            modifier = Modifier.fillMaxSize().then(chartModifier)
        )
    }

    val accumulatedChart: @Composable () -> Unit = {
        AccumulatedChart(
            xValues = xValues,
            sortedPoints = sortedPoints,
            showVolume = showVolume,
            showWeight = showWeight,
            maxX = maxX,
            syncState = syncState,
            chartId = 2,
            modifier = Modifier.fillMaxSize().then(chartModifier)
        )
    }

    if (isHeightCompact()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val childModifier = Modifier.weight(1f)
            if (showPressure) Box(modifier = childModifier) { pressureChart() }
            if (showFlowChart) Box(modifier = childModifier) { flowRateChart() }
            if (showAccumulatedChart) Box(modifier = childModifier) { accumulatedChart() }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val childModifier = Modifier.weight(1f)
            if (showPressure) Box(modifier = childModifier) { pressureChart() }
            if (showFlowChart) Box(modifier = childModifier) { flowRateChart() }
            if (showAccumulatedChart) Box(modifier = childModifier) { accumulatedChart() }
        }
    }
}

@Composable
private fun ChartsNotSelected(
    modifier: Modifier = Modifier
) {
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
private fun PressureChart(
    xValues: List<Double>,
    sortedPoints: List<ChartData>,
    targetSeries: Map<Double, Double>?,
    maxX: Double,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    val tX = targetSeries?.entries?.sortedBy { it.key }?.map { it.key }
    val tY = targetSeries?.entries?.sortedBy { it.key }?.map { it.value }

    LaunchedEffect(xValues, sortedPoints, tX, tY) {
        producer.runTransaction {
            lineSeries {
                if (xValues.isNotEmpty()) {
                    series(x = xValues, y = sortedPoints.map { it.pressure })
                } else {
                    series(x = listOf(0.0), y = listOf(0.0))
                }
                if (tX != null) {
                    if (tX.isNotEmpty() && tY != null) {
                        series(x = tX, y = tY)
                    } else {
                        series(x = listOf(0.0), y = listOf(0.0))
                    }
                }
            }
        }
    }

    val maxPressure = sortedPoints.maxOfOrNull { it.pressure.toDouble() } ?: 0.0
    val maxTarget = targetSeries?.values?.maxOrNull() ?: 0.0
    val maxY = maxOf(12.0, maxPressure, maxTarget)

    val units = buildList {
        add("bar")
        if (tX != null) add("bar")
    }
    val marker = rememberBrewChartMarker(units)
    val listener = rememberBrewMarkerVisibilityListener(syncState, chartId)
    val syncMarkerX = if (syncState.activeChartId != chartId) syncState.markerX else null

    BrewChart(
        modelProducer = producer,
        colors = listOf(MaterialTheme.colorScheme.error),
        targetColor = if (tX != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else null,
        maxX = maxX,
        maxY = maxY,
        marker = marker,
        markerVisibilityListener = listener,
        syncMarkerX = syncMarkerX,
        modifier = modifier
    )
}

@Composable
private fun FlowRateChart(
    xValues: List<Double>,
    sortedPoints: List<ChartData>,
    targetSeries: Map<Double, Double>?,
    showFlowRate: Boolean,
    showWeightRate: Boolean,
    maxX: Double,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    val tX = targetSeries?.entries?.sortedBy { it.key }?.map { it.key }
    val tY = targetSeries?.entries?.sortedBy { it.key }?.map { it.value }

    LaunchedEffect(xValues, sortedPoints, showFlowRate, showWeightRate, tX, tY) {
        producer.runTransaction {
            lineSeries {
                if (xValues.isNotEmpty()) {
                    if (showFlowRate) series(x = xValues, y = sortedPoints.map { it.volumePerSecond })
                    if (showWeightRate) series(x = xValues, y = sortedPoints.map { it.weightPerSecond })
                } else {
                    if (showFlowRate) series(x = listOf(0.0), y = listOf(0.0))
                    if (showWeightRate) series(x = listOf(0.0), y = listOf(0.0))
                }
                if (tX != null && showFlowRate) {
                    if (tX.isNotEmpty() && tY != null) {
                        series(x = tX, y = tY)
                    } else {
                        series(x = listOf(0.0), y = listOf(0.0))
                    }
                }
            }
        }
    }

    val maxRate = sortedPoints.maxOfOrNull {
        val v1 = if (showFlowRate) it.volumePerSecond.toDouble() else 0.0
        val v2 = if (showWeightRate) it.weightPerSecond.toDouble() else 0.0
        maxOf(v1, v2)
    } ?: 0.0
    val maxTarget = if (showFlowRate) targetSeries?.values?.maxOrNull() ?: 0.0 else 0.0
    val maxY = maxOf(12.0, maxRate, maxTarget)

    val colors = buildList {
        if (showFlowRate) add(GeeFlowTheme.colors.water)
        if (showWeightRate) add(MaterialTheme.colorScheme.onSurface)
    }

    val units = buildList {
        if (showFlowRate) add("ml/s")
        if (showWeightRate) add("g/s")
        if (tX != null && showFlowRate) add("ml/s")
    }
    val marker = rememberBrewChartMarker(units)
    val listener = rememberBrewMarkerVisibilityListener(syncState, chartId)
    val syncMarkerX = if (syncState.activeChartId != chartId) syncState.markerX else null

    BrewChart(
        modelProducer = producer,
        colors = colors,
        targetColor = if (tX != null && showFlowRate) GeeFlowTheme.colors.water.copy(alpha = 0.5f) else null,
        maxX = maxX,
        maxY = maxY,
        marker = marker,
        markerVisibilityListener = listener,
        syncMarkerX = syncMarkerX,
        modifier = modifier
    )
}

@Composable
private fun AccumulatedChart(
    xValues: List<Double>,
    sortedPoints: List<ChartData>,
    showVolume: Boolean,
    showWeight: Boolean,
    maxX: Double,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, sortedPoints, showVolume, showWeight) {
        producer.runTransaction {
            lineSeries {
                if (xValues.isNotEmpty()) {
                    if (showVolume) series(x = xValues, y = sortedPoints.map { it.volume })
                    if (showWeight) series(x = xValues, y = sortedPoints.map { it.weight })
                } else {
                    if (showVolume) series(x = listOf(0.0), y = listOf(0.0))
                    if (showWeight) series(x = listOf(0.0), y = listOf(0.0))
                }
            }
        }
    }

    val maxVolumeAndWeight = sortedPoints.maxOfOrNull {
        val v1 = if (showVolume) it.volume.toDouble() else 0.0
        val v2 = if (showWeight) it.weight.toDouble() else 0.0
        maxOf(v1, v2)
    } ?: 0.0
    val maxY = maxOf(40.0, maxVolumeAndWeight)

    val colors = buildList {
        if (showVolume) add(GeeFlowTheme.colors.waterVariant)
        if (showWeight) add(MaterialTheme.colorScheme.onSurface)
    }

    val units = buildList {
        if (showVolume) add("ml")
        if (showWeight) add("g")
    }
    val marker = rememberBrewChartMarker(units)
    val listener = rememberBrewMarkerVisibilityListener(syncState, chartId)
    val syncMarkerX = if (syncState.activeChartId != chartId) syncState.markerX else null

    BrewChart(
        modelProducer = producer,
        colors = colors,
        targetColor = null,
        maxX = maxX,
        maxY = maxY,
        marker = marker,
        markerVisibilityListener = listener,
        syncMarkerX = syncMarkerX,
        modifier = modifier
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
    val persistentMarkers: (CartesianChart.PersistentMarkerScope.(ExtraStore) -> Unit)? =
        syncMarkerX?.let { x -> { marker at x } }
    CartesianChartHost(
        zoomState = zoomState,
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineProvider.series(
                    *colors.map { color ->
                        LineCartesianLayer.rememberLine(
                            fill = LineFill.single(Fill(color)),
                            areaFill = AreaFill.single(Fill(Brush.verticalGradient(listOf(color.disabled(), Color.Transparent))))
                        )
                    }.toTypedArray() + listOfNotNull(
                        targetColor?.let { color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineFill.single(Fill(color)),
                                stroke = LineCartesianLayer.LineStroke.Dashed(),
                                areaFill = null
                            )
                        }
                    ).toTypedArray()
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = 0.0,
                    maxX = maxX,
                    minY = 0.0,
                    maxY = maxY
                )
            ),
            getXStep = { (maxX / 20).coerceAtLeast(0.1) },
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy()
                    )
                ),
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 2 }),
                valueFormatter = { _, value, _ -> value.toLong().toString() },
                guideline = rememberAxisGuidelineComponent(
                    shape = DashedShape(
                        shape = CircleShape,
                        dashLength = 1.dp,
                        gapLength = 5.dp
                    )
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
