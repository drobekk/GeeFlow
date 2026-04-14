package dev.drobek.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Logo: ImageVector
    get() {
        if (_AppLogo != null) {
            return _AppLogo!!
        }
        _AppLogo = ImageVector.Builder(
            name = "AppLogo",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(512f, -0f)
                curveTo(512f, -0f, 568.3f, 67.6f, 664f, 155f)
                curveTo(781.2f, 262f, 942.1f, 406.8f, 942.1f, 593.9f)
                curveTo(942.1f, 831.3f, 749.4f, 1024f, 512f, 1024f)
                curveTo(274.6f, 1024f, 81.9f, 831.3f, 81.9f, 593.9f)
                curveTo(81.9f, 411.4f, 242.1f, 259f, 356.9f, 152.1f)
                curveTo(455.9f, 59.8f, 512f, -0f, 512f, -0f)
                close()
                moveTo(509.8f, 116f)
                curveTo(484.3f, 141.2f, 451.2f, 173.3f, 411.2f, 210.5f)
                curveTo(357.1f, 260.9f, 292f, 321.8f, 240.7f, 391.8f)
                curveTo(196.4f, 452.3f, 161.7f, 519.6f, 161.7f, 593.9f)
                curveTo(161.7f, 787.2f, 318.7f, 944.2f, 512f, 944.2f)
                curveTo(705.3f, 944.2f, 862.3f, 787.2f, 862.3f, 593.9f)
                curveTo(862.3f, 518f, 827.7f, 451.5f, 783.2f, 392.4f)
                curveTo(731.4f, 323.5f, 665.5f, 264.4f, 610.2f, 213.9f)
                curveTo(569.6f, 176.8f, 535.7f, 143f, 509.8f, 116f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(550.1f, 995.2f)
                curveTo(548.7f, 999.5f, 546.6f, 1003.6f, 543.8f, 1007.4f)
                curveTo(530.4f, 1024.9f, 505.4f, 1028.3f, 487.9f, 1015f)
                curveTo(474.1f, 1004.6f, 459.5f, 982.4f, 481.7f, 952.1f)
                curveTo(487.9f, 943.6f, 503.1f, 929f, 519.5f, 908.7f)
                curveTo(539f, 884.5f, 561.4f, 850.2f, 564.2f, 805.6f)
                curveTo(567.4f, 753.2f, 543.1f, 689.1f, 466.3f, 611.4f)
                curveTo(190f, 331.5f, 484.3f, 45.4f, 484.3f, 45.4f)
                curveTo(500.1f, 30.1f, 525.4f, 30.5f, 540.7f, 46.4f)
                curveTo(556f, 62.2f, 555.6f, 87.5f, 539.7f, 102.8f)
                curveTo(539.7f, 102.8f, 302.1f, 331.4f, 523.1f, 555.3f)
                curveTo(622.8f, 656.3f, 648f, 742.5f, 643.8f, 810.5f)
                curveTo(638.4f, 898.8f, 567.6f, 977.1f, 550.1f, 995.2f)
                close()
                moveTo(545.4f, 961.5f)
                curveTo(543.1f, 957.8f, 540f, 954.4f, 536.1f, 951.5f)
                curveTo(539.9f, 954.3f, 543f, 957.7f, 545.4f, 961.5f)
                close()
            }
        }.build()

        return _AppLogo!!
    }

@Suppress("ObjectPropertyName")
private var _AppLogo: ImageVector? = null
