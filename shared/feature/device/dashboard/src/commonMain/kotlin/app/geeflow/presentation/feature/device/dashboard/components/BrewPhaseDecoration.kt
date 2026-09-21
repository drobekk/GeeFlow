package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.presentation.feature.device.dashboard.model.ChartPhaseBoundary
import app.geeflow.ui.icons.Flow
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.icons.Volume
import app.geeflow.ui.icons.Weight
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

@Composable
internal fun brewPhaseDecoration(boundaries: List<ChartPhaseBoundary>): Decoration {
    val drawScope = remember { CanvasDrawScope() }
    val colors = MaterialTheme.colorScheme
    val label = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(color = colors.onSurface),
    )
    val time = rememberVectorPainter(Icons.Outlined.Timer)
    val volume = rememberVectorPainter(GeeFlowIcon.Volume)
    val weight = rememberVectorPainter(GeeFlowIcon.Weight)
    val flow = rememberVectorPainter(GeeFlowIcon.Flow)
    val pressure = rememberVectorPainter(GeeFlowIcon.Pressure)

    return object : Decoration {
        override fun drawOverLayers(context: CartesianDrawingContext) = with(context) {
            val direction = layoutDirectionMultiplier
            val start = (if (isLtr) layerBounds.left else layerBounds.right) +
                direction * layerDimensions.startPadding - scroll

            val groupedBoundaries = boundaries.groupBy { it.seconds }

            groupedBoundaries.forEach { (seconds, overlappingBoundaries) ->
                val x = start + direction * layerDimensions.xSpacing *
                    ((seconds - ranges.minX) / ranges.xStep).toFloat()
                if (x < layerBounds.left - 1 || x > layerBounds.right + 1) return@forEach

                overlappingBoundaries.forEachIndexed { index, boundary ->
                    val tint = if (boundary.actual) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.45f)
                    drawScope.draw(density = density, layoutDirection = layoutDirection, canvas = canvas, size = canvasSize) {
                        drawLine(
                            color = tint,
                            start = Offset(x, layerBounds.top),
                            end = Offset(x, layerBounds.bottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (boundary.actual) {
                                null
                            } else {
                                PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        4.dp.toPx(),
                                        3.dp.toPx(),
                                    ),
                                )
                            },
                        )
                    }

                    val offsetIndex = index - (overlappingBoundaries.size - 1) / 2f
                    val offsetX = offsetIndex * 20.dp.pixels
                    val centerX = (x + offsetX).coerceIn(layerBounds.left + 12.dp.pixels, layerBounds.right - 12.dp.pixels)

                    var currentY = layerBounds.top + 12.dp.pixels

                    boundary.stepNumber?.let { number ->
                        if (currentY + 8.dp.pixels <= layerBounds.bottom) {
                            drawScope.draw(
                                density = density,
                                layoutDirection = layoutDirection,
                                canvas = canvas,
                                size = canvasSize,
                            ) {
                                drawCircle(
                                    color = colors.surfaceContainerHigh,
                                    radius = 8.dp.toPx(),
                                    center = Offset(centerX, currentY),
                                )
                            }
                            label.draw(
                                context = context,
                                text = number.toString(),
                                x = centerX,
                                y = currentY,
                                horizontalPosition = Position.Horizontal.Center,
                                verticalPosition = Position.Vertical.Center,
                            )
                        }
                        currentY += 18.dp.pixels
                    }

                    boundary.conditions.forEach { condition ->
                        if (currentY + 8.dp.pixels <= layerBounds.bottom) {
                            val painter = when (condition.metric) {
                                BrewMetric.PhaseTime -> time
                                BrewMetric.PumpedVolume -> volume
                                BrewMetric.CupWeight -> weight
                                BrewMetric.PumpFlow -> flow
                                BrewMetric.PumpPressure, BrewMetric.GroupPressure, BrewMetric.BoilerPressure -> pressure
                            }
                            val selected = condition in boundary.matchedConditions
                            drawScope.draw(
                                density = density,
                                layoutDirection = layoutDirection,
                                canvas = canvas,
                                size = canvasSize,
                            ) {
                                drawCircle(
                                    color = if (selected) colors.primary else colors.surfaceContainerHigh,
                                    radius = 8.dp.toPx(),
                                    center = Offset(centerX, currentY),
                                )
                                translate(left = centerX - 6.dp.toPx(), top = currentY - 6.dp.toPx()) {
                                    with(painter) {
                                        draw(
                                            size = Size(12.dp.toPx(), 12.dp.toPx()),
                                            colorFilter = ColorFilter.tint(
                                                if (selected) {
                                                    colors.onPrimary
                                                } else {
                                                    colors.onSurfaceVariant
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                        currentY += 18.dp.pixels
                    }
                }
            }
        }
    }
}
