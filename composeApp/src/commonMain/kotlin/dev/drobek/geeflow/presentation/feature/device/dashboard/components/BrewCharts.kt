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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.FlowRate
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Pressure
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Volume
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.Weight
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType.WeightRate
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

@Composable
internal fun BrewCharts(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    modifier: Modifier = Modifier
) {
    BrewChartsSection(
        brew = brew,
        visibleCharts = visibleCharts,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
internal fun BrewChartsSection(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    modifier: Modifier = Modifier
) {
    val chartModifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
        .fillMaxWidth()
        .padding(16.dp)

    val sortedEntries = brew.data.entries.sortedBy { it.key }
    if (sortedEntries.isEmpty()) {
        ChartsNotSelected(modifier = modifier.then(chartModifier))
        return
    }

    val xValues = sortedEntries.map { it.key.toDouble() }
    val sortedPoints = sortedEntries.map { it.value }
    val maxX = maxOf(xValues.maxOrNull() ?: 0.0, 20.0)



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
            maxX = maxX,
            modifier = Modifier.fillMaxSize().then(chartModifier)
        )
    }

    val flowRateChart: @Composable () -> Unit = {
        FlowRateChart(
            xValues = xValues,
            sortedPoints = sortedPoints,
            showFlowRate = showFlowRate,
            showWeightRate = showWeightRate,
            maxX = maxX,
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
            modifier = Modifier.fillMaxSize().then(chartModifier)
        )
    }

    if (isHeightCompact()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifier = Modifier.weight(1f)
            if (showPressure) Box(modifier = modifier) { pressureChart() }
            if (showFlowChart) Box(modifier = modifier) { flowRateChart() }
            if (showAccumulatedChart) Box(modifier = modifier) { accumulatedChart() }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifier = Modifier.weight(1f)
            if (showPressure) Box(modifier = modifier) { pressureChart() }
            if (showFlowChart) Box(modifier = modifier) { flowRateChart() }
            if (showAccumulatedChart) Box(modifier = modifier) { accumulatedChart() }
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
    sortedPoints: List<Brew.Data>,
    maxX: Double,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, sortedPoints) {
        producer.runTransaction {
            lineSeries {
                series(x = xValues, y = sortedPoints.map { it.pressure })
            }
        }
    }

    val maxPressure = sortedPoints.maxOf { it.pressure.toDouble() }
    val maxY = maxOf(12.0, maxPressure)

    BrewChart(
        modelProducer = producer,
        colors = listOf(MaterialTheme.colorScheme.error),
        maxX = maxX,
        maxY = maxY,
        modifier = modifier
    )
}

@Composable
private fun FlowRateChart(
    xValues: List<Double>,
    sortedPoints: List<Brew.Data>,
    showFlowRate: Boolean,
    showWeightRate: Boolean,
    maxX: Double,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, sortedPoints, showFlowRate, showWeightRate) {
        if (showFlowRate || showWeightRate) {
            producer.runTransaction {
                lineSeries {
                    if (showFlowRate) series(x = xValues, y = sortedPoints.map { it.volumePerSecond })
                    if (showWeightRate) series(x = xValues, y = sortedPoints.map { it.weightPerSecond })
                }
            }
        }
    }

    val maxRate = sortedPoints.maxOf {
        val v1 = if (showFlowRate) it.volumePerSecond.toDouble() else 0.0
        val v2 = if (showWeightRate) it.weightPerSecond.toDouble() else 0.0
        maxOf(v1, v2)
    }
    val maxY = maxOf(12.0, maxRate)

    val colors = buildList {
        if (showFlowRate) add(GeeFlowTheme.colors.water)
        if (showWeightRate) add(MaterialTheme.colorScheme.onSurface)
    }

    BrewChart(
        modelProducer = producer,
        colors = colors,
        maxX = maxX,
        maxY = maxY,
        modifier = modifier
    )
}

@Composable
private fun AccumulatedChart(
    xValues: List<Double>,
    sortedPoints: List<Brew.Data>,
    showVolume: Boolean,
    showWeight: Boolean,
    maxX: Double,
    modifier: Modifier = Modifier
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, sortedPoints, showVolume, showWeight) {
        if (showVolume || showWeight) {
            producer.runTransaction {
                lineSeries {
                    if (showVolume) series(x = xValues, y = sortedPoints.map { it.volume })
                    if (showWeight) series(x = xValues, y = sortedPoints.map { it.weight })
                }
            }
        }
    }

    val maxVolumeAndWeight = sortedPoints.maxOf {
        val v1 = if (showVolume) it.volume.toDouble() else 0.0
        val v2 = if (showWeight) it.weight.toDouble() else 0.0
        maxOf(v1, v2)
    }
    val maxY = maxOf(40.0, maxVolumeAndWeight)

    val colors = buildList {
        if (showVolume) add(GeeFlowTheme.colors.waterVariant)
        if (showWeight) add(MaterialTheme.colorScheme.onSurface)
    }

    BrewChart(
        modelProducer = producer,
        colors = colors,
        maxX = maxX,
        maxY = maxY,
        modifier = modifier
    )
}

@Composable
private fun BrewChart(
    modelProducer: CartesianChartModelProducer,
    colors: List<Color>,
    maxX: Double,
    maxY: Double,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty()) return

    val vicoScrollState = rememberVicoScrollState(scrollEnabled = false)
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = Zoom.Content,
        minZoom = Zoom.Content,
        maxZoom = Zoom.Content,
    )
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
                    }.toTypedArray()
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = 0.0,
                    maxX = maxX,
                    minY = 0.0,
                    maxY = maxY
                )
            ),
            getXStep = { (maxX / 20).coerceAtLeast(0.1) },
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

@Composable
internal fun BrewData(
    brew: Brew,
    visibleCharts: Set<DashboardChartType>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedPoints = brew.data.values
    val avgPressure = if (sortedPoints.isNotEmpty()) sortedPoints.map { it.pressure }.average() else 0.0
    val avgFlow = if (sortedPoints.isNotEmpty()) sortedPoints.map { it.volumePerSecond }.average() else 0.0
    val avgWeightRate = if (sortedPoints.isNotEmpty()) sortedPoints.map { it.weightPerSecond }.average() else 0.0
    val totalWeight = brew.data.values.lastOrNull()?.weight ?: 0f
    val totalVolume = brew.data.values.lastOrNull()?.volume ?: 0f

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
                        value = avgPressure,
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
                        value = avgWeightRate,
                        unit = stringResource(Res.string.unit_grams_per_second),
                        color = MaterialTheme.colorScheme.onSurface,
                        type = WeightRate,
                        showDivider = visibleCharts.contains(WeightRate)
                    ),
                    ValueEntry(
                        value = avgFlow,
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
            .clip(RoundedCornerShape(8.dp))
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
