package dev.drobek.geeflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.scrollFade(
    scrollState: ScrollState,
    height: Dp = 25.dp,
    color: Color = MaterialTheme.colorScheme.background,
    position: FadePosition = FadePosition.Top,
): Modifier {
    val showTop = (scrollState.value > 0) && (position == FadePosition.Top || position == FadePosition.Both)
    val showBottom =
        (scrollState.value < scrollState.maxValue) && (position == FadePosition.Bottom || position == FadePosition.Both)
    return scrollFade(showTop, showBottom, height, color)
}

@Composable
fun Modifier.scrollFade(
    listState: LazyListState,
    height: Dp = 25.dp,
    color: Color = MaterialTheme.colorScheme.background,
    position: FadePosition = FadePosition.Top,
): Modifier {
    val atTopState =
        remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
    val atEndState = remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) true else {
                val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
                if (lastVisible.index < total - 1) {
                    false
                } else {
                    lastVisible.offset + lastVisible.size <= info.viewportEndOffset
                }
            }
        }
    }

    val showTop = !atTopState.value && (position == FadePosition.Top || position == FadePosition.Both)
    val showBottom = !atEndState.value && (position == FadePosition.Bottom || position == FadePosition.Both)

    return scrollFade(showTop, showBottom, height, color)
}

@Composable
fun Modifier.scrollFade(
    showTop: Boolean,
    showBottom: Boolean,
    height: Dp,
    color: Color
): Modifier = composed {
    val topAlpha = animateFloatAsState(
        targetValue = if (showTop) 1f else 0f,
        animationSpec = tween(durationMillis = AnimationSpeedMs),
        label = "ScrollFadeTopAlpha"
    ).value

    val bottomAlpha = animateFloatAsState(
        targetValue = if (showBottom) 1f else 0f,
        animationSpec = tween(durationMillis = AnimationSpeedMs),
        label = "ScrollFadeBottomAlpha"
    ).value

    drawWithContent {
        drawContent()
        val heightPx = height.toPx()
        if (topAlpha > 0f) {
            val brush = verticalGradient(
                colors = listOf(color.copy(alpha = topAlpha), color.copy(alpha = 0f)),
                startY = 0f,
                endY = heightPx
            )
            drawRect(
                brush = brush,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, heightPx),
                blendMode = BlendMode.SrcOver
            )
        }
        if (bottomAlpha > 0f) {
            val brush = verticalGradient(
                colors = listOf(color.copy(alpha = 0f), color.copy(alpha = bottomAlpha)),
                startY = size.height - heightPx,
                endY = size.height
            )
            drawRect(
                brush = brush,
                topLeft = Offset(0f, size.height - heightPx),
                size = Size(size.width, heightPx),
                blendMode = BlendMode.SrcOver
            )
        }
    }
}

@Composable
fun Modifier.verticalScrollWithFade(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    dividerHeight: Dp = 25.dp,
    color: Color = MaterialTheme.colorScheme.background,
    fadePosition: FadePosition = FadePosition.Top
) = scrollFade(state, dividerHeight, color, fadePosition)
    .verticalScroll(state, enabled, flingBehavior, reverseScrolling)

private const val AnimationSpeedMs = 200

enum class FadePosition { Top, Bottom, Both }
