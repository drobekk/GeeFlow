package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Flush: ImageVector
    get() {
        if (_Flush != null) {
            return _Flush!!
        }

        _Flush = ImageVector.Builder(
            name = "Flush",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                // Top rounded rectangle
                moveTo(160f, 140f)
                horizontalLineTo(800f)
                curveTo(822f, 140f, 840f, 158f, 840f, 180f)
                verticalLineTo(200f)
                curveTo(840f, 222f, 822f, 240f, 800f, 240f)
                horizontalLineTo(160f)
                curveTo(138f, 240f, 120f, 222f, 120f, 200f)
                verticalLineTo(180f)
                curveTo(120f, 158f, 138f, 140f, 160f, 140f)
                close()

                // Left stream
                moveTo(291f, 424f)
                lineTo(206f, 679f)
                curveTo(200f, 698f, 210f, 718f, 229f, 724f)
                curveTo(248f, 730f, 268f, 720f, 274f, 701f)
                lineTo(359f, 446f)
                curveTo(365f, 427f, 355f, 407f, 336f, 401f)
                curveTo(317f, 395f, 297f, 405f, 291f, 424f)
                close()

                // Center stream
                moveTo(480f, 390f)
                curveTo(459f, 390f, 442f, 407f, 442f, 428f)
                verticalLineTo(732f)
                curveTo(442f, 753f, 459f, 770f, 480f, 770f)
                curveTo(501f, 770f, 518f, 753f, 518f, 732f)
                verticalLineTo(428f)
                curveTo(518f, 407f, 501f, 390f, 480f, 390f)
                close()

                // Right stream — mirrored left stream
                moveTo(669f, 424f)
                lineTo(754f, 679f)
                curveTo(760f, 698f, 750f, 718f, 731f, 724f)
                curveTo(712f, 730f, 692f, 720f, 686f, 701f)
                lineTo(601f, 446f)
                curveTo(595f, 427f, 605f, 407f, 624f, 401f)
                curveTo(643f, 395f, 663f, 405f, 669f, 424f)
                close()
            }
        }.build()

        return _Flush!!
    }

@Suppress("ObjectPropertyName")
private var _Flush: ImageVector? = null
