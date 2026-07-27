package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Pressure: ImageVector
    get() {
        if (_Pressure != null) {
            return _Pressure!!
        }
        _Pressure = ImageVector.Builder(
            name = "Pressure",
            defaultWidth = 56.dp,
            defaultHeight = 56.dp,
            viewportWidth = 56f,
            viewportHeight = 56f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(25.464f, 4.503f)
                lineTo(30.536f, 4.503f)
                lineTo(30.536f, 17.292f)
                lineTo(34.557f, 13.272f)
                lineTo(38.143f, 16.857f)
                lineTo(28f, 27f)
                lineTo(17.857f, 16.857f)
                lineTo(21.443f, 13.272f)
                lineTo(25.464f, 17.292f)
                lineTo(25.464f, 4.503f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0f,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 5.79f
            ) {
                moveTo(10.699f, 32.466f)
                lineTo(28f, 32.466f)
                lineTo(45.301f, 32.466f)
                curveTo(45.301f, 41.696f, 37.555f, 49.18f, 28f, 49.18f)
                curveTo(18.445f, 49.18f, 10.699f, 41.696f, 10.699f, 32.466f)
                close()
            }
        }.build()

        return _Pressure!!
    }

@Suppress("ObjectPropertyName")
private var _Pressure: ImageVector? = null
