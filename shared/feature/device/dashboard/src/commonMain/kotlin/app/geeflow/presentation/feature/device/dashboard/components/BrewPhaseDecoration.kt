package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.model.ChartPhaseBoundary
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

@Composable
internal fun brewPhaseDecoration(boundaries: List<ChartPhaseBoundary>, showLabels: Boolean = true): Decoration {
    val drawScope = remember { CanvasDrawScope() }
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    return object : Decoration {
        override fun drawOverLayers(context: CartesianDrawingContext) = with(context) {
            val direction = layoutDirectionMultiplier
            val start = (if (isLtr) layerBounds.left else layerBounds.right) +
                direction * layerDimensions.startPadding - scroll
            boundaries.map { it.seconds }.distinct().sorted().forEach { seconds ->
                val x = start + direction * layerDimensions.xSpacing *
                    ((seconds - ranges.minX) / ranges.xStep).toFloat()
                if (x > layerBounds.left && x < layerBounds.right) {
                    drawScope.draw(density = density, layoutDirection = layoutDirection, canvas = canvas, size = canvasSize) {
                        drawLine(tint, Offset(x, layerBounds.top), Offset(x, layerBounds.bottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                    }
                }
            }
        }
    }
}

private const val LabelCharacterWidth = 7
private const val LabelPadding = 8
private const val LabelGap = 12
private const val LabelBoundaryGap = 3
private const val ActualBoundaryAlpha = 0.72f
private const val MaxPhaseLabelCharacters = 23
