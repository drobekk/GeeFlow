package app.geeflow.ui.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.fadeEdge(
    scrollState: ScrollState,
    color: Color = Color.Unspecified,
    width: Dp = 24.dp,
    top: Boolean = true,
    bottom: Boolean = true,
): Modifier = composed {
    val actualColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.surface else color
    this.drawWithContent {
        drawContent()

        val showTop = top && scrollState.value > 0
        val showBottom = bottom && scrollState.value < scrollState.maxValue

        if (showTop) {
            val topBrush = Brush.verticalGradient(
                0f to actualColor,
                1f to Color.Transparent,
                startY = 0f,
                endY = width.toPx(),
            )
            drawRect(
                brush = topBrush,
                size = Size(size.width, width.toPx()),
            )
        }

        if (showBottom) {
            val bottomBrush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to actualColor,
                startY = size.height - width.toPx(),
                endY = size.height,
            )
            drawRect(
                brush = bottomBrush,
                topLeft = Offset(0f, size.height - width.toPx()),
                size = Size(size.width, width.toPx()),
            )
        }
    }
}

fun Modifier.verticalScrollWithFade(
    scrollState: ScrollState,
    fadeColor: Color = Color.Unspecified,
    fadeWidth: Dp = 24.dp,
    fadeTop: Boolean = true,
    fadeBottom: Boolean = true,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
): Modifier = this
    .fadeEdge(
        scrollState = scrollState,
        color = fadeColor,
        width = fadeWidth,
        top = fadeTop,
        bottom = fadeBottom,
    )
    .verticalScroll(
        state = scrollState,
        enabled = enabled,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling,
    )
