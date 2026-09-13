package app.geeflow.ui.modifier

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlin.math.max
import kotlin.math.min

/**
 * Makes the content square in size.
 *
 * This is achieved by cropping incoming max constraints to the largest possible square size
 * and measuring the content using resulting constraints.
 * Next the size of layout is decided based on largest dimension of the measured content.
 * Finally the content is placed inside the square layout according to specified [position].
 *
 * If no square exists that falls within the size range of the incoming constraints,
 * the content will be laid out as usual, as if the modifier was not applied.
 *
 * @param position The fraction of the content's position inside its square layout.
 * It determines the point on the axis that was extended to make a square.
 * Typically you'd want to use values between `0` and `1`, inclusive, where `0`
 * will place the content at the "start" of the square, `0.5` in the middle, and `1` at the "end".
 */

// https://medium.com/@mmolosay/square-size-in-jetpack-compose-2d66457a83ea
@Stable
fun Modifier.squareSize(
    position: Float = 0.5f,
): Modifier = this.then(
    when {
        position == CenterPosition -> SquareSizeCenter
        else -> createSquareSizeModifier(position = position)
    },
)

private const val CenterPosition = 0.5f

private val SquareSizeCenter = createSquareSizeModifier(position = CenterPosition)

private class SquareSizeModifier(
    private val position: Float,
    inspectorInfo: InspectorInfo.() -> Unit,
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val maxSquare = min(constraints.maxWidth, constraints.maxHeight)
        val minSquare = max(constraints.minWidth, constraints.minHeight)
        val squareExists = (minSquare <= maxSquare)

        val resolvedConstraints = constraints
            .takeUnless { squareExists }
            ?: constraints.copy(maxWidth = maxSquare, maxHeight = maxSquare)

        val placeable = measurable.measure(resolvedConstraints)

        return if (squareExists) {
            val size = max(placeable.width, placeable.height)
            layout(size, size) {
                val x = ((size - placeable.width) * position).toInt()
                val y = ((size - placeable.height) * position).toInt()
                placeable.placeRelative(x, y)
            }
        } else {
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        val contentWidth = measurable.minIntrinsicWidth(height)
        val contentHeight = measurable.maxIntrinsicHeight(Constraints.Infinity)
        val desired = max(contentWidth, contentHeight)
        return if (height != Constraints.Infinity) desired.coerceAtMost(height) else desired
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        val contentWidth = measurable.maxIntrinsicWidth(height)
        val contentHeight = measurable.maxIntrinsicHeight(Constraints.Infinity)
        val desired = max(contentWidth, contentHeight)
        return if (height != Constraints.Infinity) desired.coerceAtMost(height) else desired
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        val contentHeight = measurable.minIntrinsicHeight(width)
        val contentWidth = measurable.maxIntrinsicWidth(Constraints.Infinity)
        val desired = max(contentWidth, contentHeight)
        return if (width != Constraints.Infinity) desired.coerceAtMost(width) else desired
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        val contentHeight = measurable.maxIntrinsicHeight(width)
        val contentWidth = measurable.maxIntrinsicWidth(Constraints.Infinity)
        val desired = max(contentWidth, contentHeight)
        return if (width != Constraints.Infinity) desired.coerceAtMost(width) else desired
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquareSizeModifier) return false

        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int = position.hashCode()
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun createSquareSizeModifier(
    position: Float,
) =
    SquareSizeModifier(
        position = position,
        inspectorInfo = debugInspectorInfo {
            name = "squareSize"
            properties["position"] = position
        },
    )
