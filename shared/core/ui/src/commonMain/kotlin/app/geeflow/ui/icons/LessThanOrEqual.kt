package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.LessThanOrEqual: ImageVector
    get() {
        if (_LessThanOrEqual != null) {
            return _LessThanOrEqual!!
        }
        _LessThanOrEqual = ImageVector.Builder(
            name = "LessThanOrEqual",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.5f, 2.27f)
                lineTo(5f, 10.14f)
                lineTo(18.5f, 18f)
                lineTo(19.5f, 16.27f)
                lineTo(8.97f, 10.14f)
                lineTo(19.5f, 4f)
                lineTo(18.5f, 2.27f)
                moveTo(5f, 20f)
                verticalLineTo(22f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(5f)
                close()
            }
        }.build()

        return _LessThanOrEqual!!
    }

@Suppress("ObjectPropertyName")
private var _LessThanOrEqual: ImageVector? = null
