package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.DeviceHub: ImageVector
    get() {
        if (_DeviceHub != null) {
            return _DeviceHub!!
        }
        _DeviceHub = ImageVector.Builder(
            name = "DeviceHub",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(2.011f, 21.989f)
                lineTo(2.011f, 15f)
                lineTo(8.092f, 15f)
                lineTo(11f, 12.174f)
                lineTo(11.028f, 9.809f)
                curveTo(10.228f, 9.52f, 9.222f, 9.037f, 8.733f, 8.359f)
                curveTo(8.244f, 7.681f, 8f, 6.92f, 8f, 6.076f)
                curveTo(8f, 4.965f, 8.389f, 4.02f, 9.167f, 3.242f)
                curveTo(9.944f, 2.465f, 10.889f, 2.076f, 12f, 2.076f)
                curveTo(13.111f, 2.076f, 14.056f, 2.465f, 14.833f, 3.242f)
                curveTo(15.611f, 4.02f, 16f, 4.965f, 16f, 6.076f)
                curveTo(16f, 6.92f, 15.756f, 7.681f, 15.267f, 8.359f)
                curveTo(14.778f, 9.037f, 13.814f, 9.563f, 13.014f, 9.852f)
                lineTo(13f, 12.174f)
                lineTo(15.951f, 15f)
                lineTo(21.989f, 15f)
                lineTo(21.989f, 21.989f)
                lineTo(15f, 21.989f)
                lineTo(14.944f, 16.939f)
                lineTo(12f, 13.983f)
                lineTo(9f, 16.96f)
                lineTo(9f, 21.989f)
                lineTo(2.011f, 21.989f)
                close()
                moveTo(12f, 4.129f)
                curveTo(10.926f, 4.129f, 10.053f, 5.002f, 10.053f, 6.076f)
                curveTo(10.053f, 7.151f, 10.926f, 8.023f, 12f, 8.023f)
                curveTo(13.074f, 8.023f, 13.947f, 7.151f, 13.947f, 6.076f)
                curveTo(13.947f, 5.002f, 13.074f, 4.129f, 12f, 4.129f)
                close()
                moveTo(7.266f, 16.844f)
                lineTo(3.722f, 16.844f)
                lineTo(3.722f, 20.388f)
                lineTo(7.266f, 20.388f)
                lineTo(7.266f, 16.844f)
                close()
                moveTo(20.278f, 16.936f)
                lineTo(16.796f, 16.936f)
                lineTo(16.796f, 20.419f)
                lineTo(20.278f, 20.419f)
                lineTo(20.278f, 16.936f)
                close()
            }
        }.build()

        return _DeviceHub!!
    }

@Suppress("ObjectPropertyName")
private var _DeviceHub: ImageVector? = null
