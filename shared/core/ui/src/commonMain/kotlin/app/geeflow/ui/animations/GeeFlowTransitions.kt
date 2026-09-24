package app.geeflow.ui.animations

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val EnterDurationMillis = 300
private const val ExitDurationMillis = 200
private const val SlideFraction = 8

val ForwardTransition: ContentTransform
    get() = slideTransform(enterFromRight = true)

val BackwardTransition: ContentTransform
    get() = slideTransform(enterFromRight = false)

fun slideTransform(enterFromRight: Boolean): ContentTransform {
    val direction = if (enterFromRight) 1 else -1
    val enter = fadeIn(tween(EnterDurationMillis)) +
        slideInHorizontally(tween(EnterDurationMillis)) { direction * it / SlideFraction }
    val exit = fadeOut(tween(ExitDurationMillis)) +
        slideOutHorizontally(tween(ExitDurationMillis)) { -direction * it / SlideFraction }
    return enter togetherWith exit
}
