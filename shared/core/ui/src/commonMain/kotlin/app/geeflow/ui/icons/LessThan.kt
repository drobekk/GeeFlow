package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.LessThan: ImageVector
    get() {
        if (_LessThan != null) return _LessThan!!
        _LessThan = ImageVector.Builder(
            name = "LessThan",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.5f, 2.27f)
                lineTo(4f, 10.14f)
                lineTo(17.5f, 18f)
                lineTo(18.5f, 16.27f)
                lineTo(7.97f, 10.14f)
                lineTo(18.5f, 4f)
                close()
            }
        }.build()
        return _LessThan!!
    }

@Suppress("ObjectPropertyName")
private var _LessThan: ImageVector? = null
