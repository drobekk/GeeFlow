package app.geeflow.app.navigation

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

/**
 * Navigation3 leaves the transition specs empty on desktop and iOS, so they are defined here
 * in common code to keep screen transitions identical on every platform.
 */
internal val ForwardTransition: ContentTransform
    get() = slideTransform(enterFromRight = true)

internal val BackwardTransition: ContentTransform
    get() = slideTransform(enterFromRight = false)

private fun slideTransform(enterFromRight: Boolean): ContentTransform {
    val direction = if (enterFromRight) 1 else -1
    val enter = fadeIn(tween(EnterDurationMillis)) +
        slideInHorizontally(tween(EnterDurationMillis)) { direction * it / SlideFraction }
    val exit = fadeOut(tween(ExitDurationMillis)) +
        slideOutHorizontally(tween(ExitDurationMillis)) { -direction * it / SlideFraction }
    return enter togetherWith exit
}
