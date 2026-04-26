package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Manual: ImageVector
    get() {
        if (_Manual != null) {
            return _Manual!!
        }
        _Manual = ImageVector.Builder(
            name = "Manual",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(120f, 840f)
                verticalLineToRelative(-720f)
                horizontalLineToRelative(162f)
                lineToRelative(198f, 522f)
                lineToRelative(196f, -522f)
                horizontalLineToRelative(164f)
                verticalLineToRelative(720f)
                lineTo(720f, 840f)
                verticalLineToRelative(-490f)
                lineTo(531f, 840f)
                lineTo(429f, 840f)
                lineTo(240f, 353f)
                verticalLineToRelative(487f)
                lineTo(120f, 840f)
                close()
            }
        }.build()

        return _Manual!!
    }

@Suppress("ObjectPropertyName")
private var _Manual: ImageVector? = null
