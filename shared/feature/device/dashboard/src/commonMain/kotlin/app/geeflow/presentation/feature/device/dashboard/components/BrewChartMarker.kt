package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerMargins
import com.patrykandpatrick.vico.compose.cartesian.marker.CandlestickCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.LayeredComponent
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.Component
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
internal fun rememberBrewChartMarker(units: List<String>): CartesianMarker {
    val labelBackground = rememberShapeComponent(
        fill = Fill(MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.small,
    )
    val label = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
        lineCount = units.size + 1,
        padding = Insets(8.dp, 4.dp),
        background = labelBackground,
    )

    val guideline = rememberAxisGuidelineComponent(
        fill = Fill(MaterialTheme.colorScheme.outlineVariant),
    )
    val indicatorFrontComponent = rememberShapeComponent(
        Fill(MaterialTheme.colorScheme.surface),
        MaterialTheme.shapes.small,
    )
    val valueFormatter = remember(units) { BrewMarkerValueFormatter(units) }

    val indicator: (Color) -> Component = { color ->
        LayeredComponent(
            back = ShapeComponent(Fill(color.copy(alpha = 0.15f)), CircleShape),
            front = LayeredComponent(
                back = ShapeComponent(fill = Fill(color), shape = CircleShape),
                front = indicatorFrontComponent,
                padding = Insets(5.dp),
            ),
            padding = Insets(10.dp),
        )
    }

    return remember(label, valueFormatter, indicator, guideline) {
        SideCenteredCartesianMarker(
            label = label,
            valueFormatter = valueFormatter,
            indicator = indicator,
            indicatorSize = 36.dp,
            guideline = guideline,
        )
    }
}

@Composable
internal fun rememberBrewMarkerVisibilityListener(
    syncState: BrewSyncState,
    chartId: Int,
): CartesianMarkerVisibilityListener = remember(syncState, chartId) {
    object : CartesianMarkerVisibilityListener {
        override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
            syncState.markerX = targets.firstOrNull()?.x
            syncState.activeChartId = chartId
        }

        override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
            syncState.markerX = targets.firstOrNull()?.x
            syncState.activeChartId = chartId
        }

        override fun onHidden(marker: CartesianMarker) {
            if (syncState.activeChartId == chartId) {
                syncState.markerX = null
                syncState.activeChartId = null
            }
        }
    }
}

internal class BrewSyncState {
    var markerX by mutableStateOf<Double?>(null)
    var activeChartId by mutableStateOf<Int?>(null)
}

@Composable
internal fun rememberBrewSyncState(): BrewSyncState = remember { BrewSyncState() }

private class SideCenteredCartesianMarker(
    label: TextComponent,
    valueFormatter: ValueFormatter,
    indicator: ((Color) -> Component)?,
    indicatorSize: Dp,
    guideline: LineComponent?,
) : DefaultCartesianMarker(
    label = label,
    valueFormatter = valueFormatter,
    labelPosition = LabelPosition.Top,
    indicator = indicator,
    indicatorSize = indicatorSize,
    guideline = guideline,
) {
    override fun updateLayerMargins(
        context: CartesianMeasuringContext,
        layerMargins: CartesianLayerMargins,
        layerDimensions: CartesianLayerDimensions,
        model: CartesianChartModel,
    ) {
        // No margin reserved — label is drawn inside the chart
    }

    override fun drawOverLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ) {
        with(context) {
            drawGuideline(targets)
            val halfIndicatorSize = indicatorSize.pixels / 2
            targets.forEach { target ->
                when (target) {
                    is LineCartesianLayerMarkerTarget ->
                        target.points.forEach { point ->
                            drawIndicator(target.canvasX, point.canvasY, point.color, halfIndicatorSize)
                        }

                    is ColumnCartesianLayerMarkerTarget ->
                        target.columns.forEach { col ->
                            drawIndicator(target.canvasX, col.canvasY, col.color, halfIndicatorSize)
                        }

                    is CandlestickCartesianLayerMarkerTarget -> {
                        drawIndicator(target.canvasX, target.openingCanvasY, target.openingColor, halfIndicatorSize)
                        drawIndicator(target.canvasX, target.closingCanvasY, target.closingColor, halfIndicatorSize)
                        drawIndicator(target.canvasX, target.lowCanvasY, target.lowColor, halfIndicatorSize)
                        drawIndicator(target.canvasX, target.highCanvasY, target.highColor, halfIndicatorSize)
                    }
                }
            }
            drawSideLabel(targets)
        }
    }

    private fun CartesianDrawingContext.drawSideLabel(targets: List<CartesianMarker.Target>) {
        val text = valueFormatter.format(this, targets)
        val targetX = targets.map { it.canvasX }.average().toFloat()
        val labelWidth = label.getBounds(this, text, layerBounds.width.toInt()).width
        val padding = 24.dp.pixels
        val centerY = (layerBounds.top + layerBounds.bottom) / 2

        val fitsRight = targetX + padding + labelWidth <= layerBounds.right
        val (x, hPos) = if (fitsRight) {
            targetX + padding to Position.Horizontal.End
        } else {
            targetX - padding to Position.Horizontal.Start
        }

        label.draw(
            context = this,
            text = text,
            x = x,
            y = centerY,
            horizontalPosition = hPos,
            verticalPosition = Position.Vertical.Center,
        )
    }
}

private class BrewMarkerValueFormatter(private val units: List<String>) : DefaultCartesianMarker.ValueFormatter {
    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence = buildAnnotatedString {
        val x = targets.firstOrNull()?.x ?: 0.0
        append("${x.format1dp()}s")

        val allPoints = targets
            .filterIsInstance<LineCartesianLayerMarkerTarget>()
            .flatMap { it.points }

        if (allPoints.isNotEmpty()) append("\n")

        allPoints.forEachIndexed { index, point ->
            val unit = units.getOrElse(index) { "" }
            withStyle(SpanStyle(color = point.color)) {
                append("● ${point.entry.y.format1dp()}")
            }
            if (unit.isNotEmpty()) append(" $unit")
            if (index < allPoints.lastIndex) append("\n")
        }
    }
}

private const val DecimalScale = 10

private fun Double.format1dp(): String {
    val rounded = (this * DecimalScale).roundToLong()
    val intPart = rounded / DecimalScale
    val fracPart = abs(rounded % DecimalScale)
    return "$intPart.$fracPart"
}
