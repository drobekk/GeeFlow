package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Person: ImageVector
    get() {
        if (_Person != null) {
            return _Person!!
        }
        _Person = ImageVector.Builder(
            name = "Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(8.446f, 14.242f)
                curveTo(7.46f, 13.259f, 6.968f, 12.077f, 6.968f, 10.697f)
                curveTo(6.968f, 9.317f, 7.46f, 8.135f, 8.446f, 7.152f)
                curveTo(9.431f, 6.169f, 10.616f, 5.678f, 12f, 5.678f)
                curveTo(13.384f, 5.678f, 14.569f, 6.169f, 15.554f, 7.152f)
                curveTo(16.54f, 8.135f, 17.033f, 9.317f, 17.033f, 10.697f)
                curveTo(17.033f, 12.077f, 16.54f, 13.259f, 15.554f, 14.242f)
                curveTo(14.569f, 15.225f, 13.384f, 15.717f, 12f, 15.717f)
                curveTo(10.616f, 15.717f, 9.431f, 15.225f, 8.446f, 14.242f)
                close()
                moveTo(1.935f, 25.755f)
                lineTo(1.935f, 22.242f)
                curveTo(1.935f, 21.531f, 2.118f, 20.877f, 2.485f, 20.281f)
                curveTo(2.852f, 19.685f, 3.34f, 19.23f, 3.948f, 18.916f)
                curveTo(5.248f, 18.268f, 6.569f, 17.782f, 7.911f, 17.458f)
                curveTo(9.253f, 17.133f, 10.616f, 16.971f, 12f, 16.971f)
                curveTo(13.384f, 16.971f, 14.747f, 17.133f, 16.089f, 17.458f)
                curveTo(17.431f, 17.782f, 18.752f, 18.268f, 20.052f, 18.916f)
                curveTo(20.661f, 19.23f, 21.148f, 19.685f, 21.515f, 20.281f)
                curveTo(21.882f, 20.877f, 22.065f, 21.531f, 22.065f, 22.242f)
                lineTo(22.065f, 25.755f)
                lineTo(1.935f, 25.755f)
                close()
            }
        }.build()

        return _Person!!
    }

@Suppress("ObjectPropertyName")
private var _Person: ImageVector? = null
