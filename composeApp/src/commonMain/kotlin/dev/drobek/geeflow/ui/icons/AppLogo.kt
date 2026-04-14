package dev.drobek.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.AppLogo: ImageVector
    get() {
        if (_AppLogo != null) {
            return _AppLogo!!
        }
        _AppLogo = ImageVector.Builder(
            name = "AppLogo",
            defaultWidth = 256.dp,
            defaultHeight = 256.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(fill = SolidColor(Color(0xFF624000))) {
                moveTo(128f, 128f)
                moveToRelative(-127.18f, 0f)
                arcToRelative(127.18f, 127.18f, 135f, isMoreThanHalf = true, isPositiveArc = true, 254.35f, 0f)
                arcToRelative(127.18f, 127.18f, 0f, isMoreThanHalf = true, isPositiveArc = true, -254.35f, 0f)
            }
            path(fill = SolidColor(Color(0xFFFFDDB2))) {
                moveTo(129.82f, 50.71f)
                curveTo(129.82f, 50.71f, 137.92f, 60.44f, 151.71f, 73.03f)
                curveTo(168.57f, 88.43f, 191.74f, 109.28f, 191.74f, 136.22f)
                curveTo(191.74f, 170.4f, 163.99f, 198.15f, 129.82f, 198.15f)
                curveTo(95.64f, 198.15f, 67.89f, 170.4f, 67.89f, 136.22f)
                curveTo(67.89f, 109.95f, 90.95f, 88.01f, 107.48f, 72.61f)
                curveTo(121.75f, 59.32f, 129.82f, 50.71f, 129.82f, 50.71f)
                close()
                moveTo(129.49f, 67.42f)
                curveTo(125.83f, 71.05f, 121.06f, 75.66f, 115.31f, 81.02f)
                curveTo(107.52f, 88.28f, 98.14f, 97.04f, 90.75f, 107.13f)
                curveTo(84.38f, 115.83f, 79.38f, 125.53f, 79.38f, 136.22f)
                curveTo(79.38f, 164.06f, 101.98f, 186.66f, 129.82f, 186.66f)
                curveTo(157.65f, 186.66f, 180.25f, 164.06f, 180.25f, 136.22f)
                curveTo(180.25f, 125.3f, 175.27f, 115.72f, 168.87f, 107.21f)
                curveTo(161.41f, 97.29f, 151.92f, 88.78f, 143.96f, 81.51f)
                curveTo(138.1f, 76.17f, 133.23f, 71.31f, 129.49f, 67.42f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFFDDB2))) {
                moveTo(135.3f, 194f)
                curveTo(135.1f, 194.62f, 134.8f, 195.21f, 134.39f, 195.76f)
                curveTo(132.47f, 198.28f, 128.86f, 198.78f, 126.34f, 196.86f)
                curveTo(124.36f, 195.36f, 122.26f, 192.17f, 125.46f, 187.79f)
                curveTo(126.35f, 186.57f, 128.53f, 184.47f, 130.89f, 181.55f)
                curveTo(133.71f, 178.06f, 136.93f, 173.12f, 137.33f, 166.71f)
                curveTo(137.79f, 159.16f, 134.3f, 149.94f, 123.24f, 138.74f)
                curveTo(83.45f, 98.44f, 125.82f, 57.25f, 125.82f, 57.25f)
                curveTo(128.1f, 55.05f, 131.74f, 55.11f, 133.94f, 57.39f)
                curveTo(136.15f, 59.67f, 136.09f, 63.31f, 133.81f, 65.51f)
                curveTo(133.81f, 65.51f, 99.59f, 98.43f, 131.42f, 130.67f)
                curveTo(145.77f, 145.2f, 149.4f, 157.62f, 148.79f, 167.41f)
                curveTo(148.01f, 180.13f, 137.82f, 191.4f, 135.3f, 194f)
                close()
                moveTo(134.63f, 189.15f)
                curveTo(134.29f, 188.61f, 133.85f, 188.13f, 133.29f, 187.71f)
                curveTo(133.83f, 188.12f, 134.28f, 188.61f, 134.63f, 189.15f)
                close()
            }
        }.build()

        return _AppLogo!!
    }

@Suppress("ObjectPropertyName")
private var _AppLogo: ImageVector? = null
