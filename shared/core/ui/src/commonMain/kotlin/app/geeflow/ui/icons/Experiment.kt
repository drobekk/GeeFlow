package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Experiment: ImageVector
    get() {
        if (_Experiment != null) {
            return _Experiment!!
        }
        _Experiment = ImageVector.Builder(
            name = "Experiment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(200f, 840f)
                quadToRelative(-51f, 0f, -72.5f, -45.5f)
                reflectiveQuadTo(138f, 710f)
                lineToRelative(222f, -270f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(-40f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(280f, 160f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(320f, 120f)
                horizontalLineToRelative(320f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(680f, 160f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(640f, 200f)
                horizontalLineToRelative(-40f)
                verticalLineToRelative(240f)
                lineToRelative(222f, 270f)
                quadToRelative(32f, 39f, 10.5f, 84.5f)
                reflectiveQuadTo(760f, 840f)
                lineTo(200f, 840f)
                close()
                moveTo(280f, 720f)
                horizontalLineToRelative(400f)
                lineTo(544f, 560f)
                lineTo(416f, 560f)
                lineTo(280f, 720f)
                close()
                moveTo(200f, 760f)
                horizontalLineToRelative(560f)
                lineTo(520f, 468f)
                verticalLineToRelative(-268f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(268f)
                lineTo(200f, 760f)
                close()
                moveTo(480f, 480f)
                close()
            }
        }.build()

        return _Experiment!!
    }

@Suppress("ObjectPropertyName")
private var _Experiment: ImageVector? = null
