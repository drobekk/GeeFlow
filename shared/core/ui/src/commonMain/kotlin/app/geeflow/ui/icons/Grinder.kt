package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Grinder: ImageVector
    get() {
        if (_Grinder != null) {
            return _Grinder!!
        }
        _Grinder = ImageVector.Builder(
            name = "Grinder",
            defaultWidth = 100.dp,
            defaultHeight = 100.dp,
            viewportWidth = 100f,
            viewportHeight = 100f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f
            ) {
                moveTo(50f, 50f)
                moveToRelative(-41f, 0f)
                arcToRelative(41f, 41f, 0f, isMoreThanHalf = true, isPositiveArc = true, 82f, 0f)
                arcToRelative(41f, 41f, 0f, isMoreThanHalf = true, isPositiveArc = true, -82f, 0f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f
            ) {
                moveTo(50f, 50f)
                moveToRelative(-9f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, -18f, 0f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(50f, 13f)
                lineTo(55f, 26.5f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(71.75f, 20.07f)
                lineTo(67.86f, 33.93f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(85.19f, 38.56f)
                lineTo(73.89f, 47.49f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(85.19f, 61.44f)
                lineTo(70.8f, 62.02f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(71.75f, 79.93f)
                lineTo(59.77f, 71.95f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(50f, 87f)
                lineTo(45f, 73.5f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(28.25f, 79.93f)
                lineTo(32.14f, 66.07f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14.81f, 61.44f)
                lineTo(26.11f, 52.51f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14.81f, 38.56f)
                lineTo(29.2f, 37.98f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(28.25f, 20.07f)
                lineTo(40.23f, 28.05f)
            }
        }.build()

        return _Grinder!!
    }

@Suppress("ObjectPropertyName")
private var _Grinder: ImageVector? = null
