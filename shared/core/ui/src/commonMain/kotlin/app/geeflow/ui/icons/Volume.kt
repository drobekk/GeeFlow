package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.Volume: ImageVector
    get() = VolumeIcon

private val VolumeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Volume",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(4f, 4f)
            horizontalLineTo(21f)
            lineTo(18f, 7f)
            verticalLineTo(18f)
            quadTo(18f, 20f, 16f, 20f)
            horizontalLineTo(6f)
            quadTo(4f, 20f, 4f, 18f)
            close()

            moveTo(4f, 8f)
            horizontalLineTo(9f)
            moveTo(4f, 12f)
            horizontalLineTo(8f)
            moveTo(4f, 16f)
            horizontalLineTo(9f)
        }
    }.build()
}
