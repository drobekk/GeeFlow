package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Flow: ImageVector
    get() {
        if (_Flow != null) {
            return _Flow!!
        }
        _Flow = ImageVector.Builder(
            name = "Flow",
            defaultWidth = 56.dp,
            defaultHeight = 56.dp,
            viewportWidth = 56f,
            viewportHeight = 56f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0f,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 6.45f
            ) {
                moveTo(28f, 6.562f)
                curveTo(28f, 6.562f, 44.123f, 27.965f, 44.123f, 33.316f)
                curveTo(44.123f, 42.215f, 36.898f, 49.438f, 28f, 49.438f)
                curveTo(19.102f, 49.438f, 11.877f, 42.215f, 11.877f, 33.316f)
                curveTo(11.877f, 27.965f, 28f, 6.562f, 28f, 6.562f)
                close()
            }
        }.build()

        return _Flow!!
    }

@Suppress("ObjectPropertyName")
private var _Flow: ImageVector? = null
