package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.FlowControl: ImageVector
    get() {
        if (_FlowControl != null) {
            return _FlowControl!!
        }
        _FlowControl = ImageVector.Builder(
            name = "FlowControl",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(603.5f, 766.5f)
                quadTo(560f, 723f, 560f, 660f)
                reflectiveQuadToRelative(43.5f, -106.5f)
                quadTo(647f, 510f, 710f, 510f)
                reflectiveQuadToRelative(106.5f, 43.5f)
                quadTo(860f, 597f, 860f, 660f)
                reflectiveQuadToRelative(-43.5f, 106.5f)
                quadTo(773f, 810f, 710f, 810f)
                reflectiveQuadToRelative(-106.5f, -43.5f)
                close()
                moveTo(759.5f, 709.5f)
                quadTo(780f, 689f, 780f, 660f)
                reflectiveQuadToRelative(-20.5f, -49.5f)
                quadTo(739f, 590f, 710f, 590f)
                reflectiveQuadToRelative(-49.5f, 20.5f)
                quadTo(640f, 631f, 640f, 660f)
                reflectiveQuadToRelative(20.5f, 49.5f)
                quadTo(681f, 730f, 710f, 730f)
                reflectiveQuadToRelative(49.5f, -20.5f)
                close()
                moveTo(160f, 700f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(80f)
                lineTo(160f, 700f)
                close()
                moveTo(143.5f, 406.5f)
                quadTo(100f, 363f, 100f, 300f)
                reflectiveQuadToRelative(43.5f, -106.5f)
                quadTo(187f, 150f, 250f, 150f)
                reflectiveQuadToRelative(106.5f, 43.5f)
                quadTo(400f, 237f, 400f, 300f)
                reflectiveQuadToRelative(-43.5f, 106.5f)
                quadTo(313f, 450f, 250f, 450f)
                reflectiveQuadToRelative(-106.5f, -43.5f)
                close()
                moveTo(299.5f, 349.5f)
                quadTo(320f, 329f, 320f, 300f)
                reflectiveQuadToRelative(-20.5f, -49.5f)
                quadTo(279f, 230f, 250f, 230f)
                reflectiveQuadToRelative(-49.5f, 20.5f)
                quadTo(180f, 271f, 180f, 300f)
                reflectiveQuadToRelative(20.5f, 49.5f)
                quadTo(221f, 370f, 250f, 370f)
                reflectiveQuadToRelative(49.5f, -20.5f)
                close()
                moveTo(480f, 340f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(80f)
                lineTo(480f, 340f)
                close()
                moveTo(710f, 660f)
                close()
                moveTo(250f, 300f)
                close()
            }
        }.build()

        return _FlowControl!!
    }

@Suppress("ObjectPropertyName")
private var _FlowControl: ImageVector? = null
