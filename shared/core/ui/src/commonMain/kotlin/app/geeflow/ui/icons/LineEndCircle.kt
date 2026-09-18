package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.LineEndCircle: ImageVector
    get() {
        if (_LineEndCircle != null) {
            return _LineEndCircle!!
        }
        _LineEndCircle = ImageVector.Builder(
            name = "LineEndCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(753f, 593f)
                quadToRelative(47f, -47f, 47f, -113f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(-47f, -47f, -113f, -47f)
                reflectiveQuadToRelative(-113f, 47f)
                quadToRelative(-47f, 47f, -47f, 113f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(47f, 47f, 113f, 47f)
                reflectiveQuadToRelative(113f, -47f)
                close()
                moveTo(640f, 720f)
                quadToRelative(-90f, 0f, -156.5f, -57f)
                reflectiveQuadTo(403f, 520f)
                lineTo(80f, 520f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(323f)
                quadToRelative(14f, -86f, 80.5f, -143f)
                reflectiveQuadTo(640f, 240f)
                quadToRelative(100f, 0f, 170f, 70f)
                reflectiveQuadToRelative(70f, 170f)
                quadToRelative(0f, 100f, -70f, 170f)
                reflectiveQuadToRelative(-170f, 70f)
                close()
                moveTo(640f, 480f)
                close()
            }
        }.build()

        return _LineEndCircle!!
    }

@Suppress("ObjectPropertyName")
private var _LineEndCircle: ImageVector? = null
