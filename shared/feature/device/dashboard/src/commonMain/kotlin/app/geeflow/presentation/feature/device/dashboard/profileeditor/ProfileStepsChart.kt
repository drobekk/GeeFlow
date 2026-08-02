package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.components.BrewSyncState
import app.geeflow.presentation.feature.device.dashboard.components.rememberBrewChartMarker
import app.geeflow.presentation.feature.device.dashboard.components.rememberBrewMarkerVisibilityListener
import app.geeflow.presentation.feature.device.dashboard.components.rememberBrewSyncState
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.ui.isHeightCompact
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.disabled
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart.PersistentMarkerScope
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
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
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private const val ChartXPadding = 5.0
private const val MinChartX = 20.0
private const val ChartFloorY = 12.0
private const val AxisStep = 2.0
private const val XStepDivisor = 10
private const val PressureChartId = 0
private const val FlowChartId = 1
private val ChartSpacing = 8.dp

/**
 * The profile steps as a pressure and a flow chart, stacked or side by side like the dashboard's
 * [app.geeflow.presentation.feature.device.dashboard.components.BrewCharts].
 */
@Composable
internal fun ProfileStepsChart(
    targetData: Map<Float, ChartData>,
    modifier: Modifier = Modifier,
) {
    // Without steps the charts still draw their axes and guidelines, seeded with a single flat point.
    val sortedEntries = targetData.entries.sortedBy { it.key }
    val xValues = sortedEntries.map { it.key.toDouble() }.ifEmpty { listOf(0.0) }
    val pressureValues = sortedEntries.map { it.value.pressure.toDouble() }.ifEmpty { listOf(0.0) }
    val flowValues = sortedEntries.map { it.value.volumePerSecond.toDouble() }.ifEmpty { listOf(0.0) }
    val maxX = maxOf(xValues.max() + ChartXPadding, MinChartX)

    val chartModifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
        .fillMaxWidth()
        .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)

    val syncState = rememberBrewSyncState()
    val unitBar = stringResource(CoreRes.string.unit_bar)
    val unitMlPerSecond = stringResource(CoreRes.string.unit_milliliters_per_second)

    val pressureChart: @Composable (Modifier) -> Unit = {
        StepChart(
            xValues = xValues,
            yValues = pressureValues,
            color = MaterialTheme.colorScheme.error,
            unit = unitBar,
            maxX = maxX,
            syncState = syncState,
            chartId = PressureChartId,
            modifier = it.then(chartModifier),
        )
    }
    val flowChart: @Composable (Modifier) -> Unit = {
        StepChart(
            xValues = xValues,
            yValues = flowValues,
            color = GeeFlowTheme.colors.water,
            unit = unitMlPerSecond,
            maxX = maxX,
            syncState = syncState,
            chartId = FlowChartId,
            modifier = it.then(chartModifier),
        )
    }

    if (isHeightCompact()) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(ChartSpacing),
        ) {
            pressureChart(Modifier.weight(1f).fillMaxSize())
            flowChart(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ChartSpacing),
        ) {
            pressureChart(Modifier.weight(1f).fillMaxSize())
            flowChart(Modifier.weight(1f).fillMaxSize())
        }
    }
}

@Composable
private fun StepChart(
    xValues: List<Double>,
    yValues: List<Double>,
    color: Color,
    unit: String,
    maxX: Double,
    syncState: BrewSyncState,
    chartId: Int,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xValues, yValues) {
        producer.runTransaction { lineModel { series(x = xValues, y = yValues) } }
    }

    val marker = rememberBrewChartMarker(listOf(unit))
    // Pressing one chart drives the other's marker, so both read out the same instant of the profile.
    val syncMarkerX = syncState.markerX.takeIf { syncState.activeChartId != chartId }
    val persistentMarkers: (PersistentMarkerScope.(ExtraStore) -> Unit)? =
        syncMarkerX?.let { x -> { marker at x } }

    CartesianChartHost(
        modelProducer = producer,
        modifier = modifier,
        animationSpec = null,
        animateIn = false,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(
            zoomEnabled = false,
            initialZoom = Zoom.Content,
            minZoom = Zoom.Content,
            maxZoom = Zoom.Content,
        ),
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineProvider.series(rememberChartLine(color)),
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = 0.0,
                    maxX = maxX,
                    minY = 0.0,
                    maxY = maxOf(ChartFloorY, yValues.max()),
                ),
            ),
            getXStep = { _, _, _ -> maxX / XStepDivisor },
            marker = marker,
            markerVisibilityListener = rememberBrewMarkerVisibilityListener(syncState, chartId),
            markerController = CartesianMarkerController.rememberShowOnPress(consumeMoveEvents = true),
            persistentMarkers = persistentMarkers,
            startAxis = VerticalAxis.rememberStart(
                tick = null,
                line = null,
                tickLength = 2.dp,
                label = rememberChartAxisLabel(),
                itemPlacer = VerticalAxis.ItemPlacer.step(step = { AxisStep }),
                valueFormatter = { _, value, _ -> value.toLong().toString() },
                guideline = rememberChartGuideline(),
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                tick = null,
                line = null,
                label = rememberChartAxisLabel(),
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                    spacing = { 1 },
                    shiftExtremeLines = false,
                    addExtremeLabelPadding = false,
                ),
                valueFormatter = { _, value, _ -> value.toLong().toString() },
                guideline = rememberChartGuideline(),
            ),
        ),
    )
}

/** Dashed like a profile drawn as a target over live brew data, but filled to read as the content. */
@Composable
private fun rememberChartLine(color: Color) = LineCartesianLayer.rememberLine(
    fill = LineFill.single(Fill(color)),
    stroke = LineCartesianLayer.LineStroke.Dashed(),
    areaFill = AreaFill.single(Fill(Brush.verticalGradient(listOf(color.disabled(), Color.Transparent)))),
)

@Composable
private fun rememberChartAxisLabel() = rememberAxisLabelComponent(
    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
)

@Composable
private fun rememberChartGuideline() = rememberAxisGuidelineComponent(
    shape = DashedShape(shape = CircleShape, dashLength = 1.dp, gapLength = 5.dp),
)
