package app.geeflow.ui.components

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun GeeFlowSwipeToRevealBox(
    state: SwipeToRevealBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    onDismiss: (SwipeToDismissBoxValue) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.anchoredDraggable(
            state = state.anchoredDraggableState,
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,
            flingBehavior = if (state.useFlingBehavior) {
                AnchoredDraggableDefaults.flingBehavior(
                    state = state.anchoredDraggableState,
                    positionalThreshold = state.positionalThreshold,
                )
            } else {
                null
            },
        ),
        propagateMinConstraints = true,
    ) {
        Row(content = backgroundContent, modifier = Modifier.matchParentSize())
        Row(
            content = content,
            modifier = Modifier
                // Use onSizeChanged to manually recalculate and update anchors
                .onSizeChanged { size ->
                    val newAnchors = DraggableAnchors {
                        val width = size.width.toFloat()
                        // Fixed: Changed from SwipeToDismissBoxValue to SwipeToRevealBoxValue
                        SwipeToRevealBoxValue.Settled at 0f
                        if (enableDismissFromStartToEnd) {
                            SwipeToRevealBoxValue.StartToEnd at width
                        }
                        if (enableDismissFromEndToStart) {
                            SwipeToRevealBoxValue.EndToStart at -width
                        }
                    }

                    val isInitialized = state.anchoredDraggableState.anchors.size > 0
                    val previousValue = state.currentValue
                    val targetValue = state.targetValue

                    val newTarget = when {
                        !isInitialized && newAnchors.hasPositionFor(previousValue) -> previousValue
                        newAnchors.hasPositionFor(targetValue) -> targetValue
                        else -> SwipeToRevealBoxValue.Settled
                    }

                    // Apply new anchors and target to the state
                    state.anchoredDraggableState.updateAnchors(newAnchors, newTarget)
                }
                // Apply the visual offset. Safe check for NaN before anchors are initialized.
                .offset {
                    IntOffset(
                        x = if (state.offset.isNaN()) 0 else state.offset.roundToInt(),
                        y = 0,
                    )
                },
        )
    }

    LaunchedEffect(state.settledValue, onDismiss) {
        // Only call when state is settled in a dismissed direction.
        if (state.settledValue != SwipeToRevealBoxValue.Settled) {
            onDismiss(state.dismissDirection)
        }
    }
}

enum class SwipeToRevealBoxValue {
    /** Can be dismissed by swiping in the reading direction. */
    StartToEnd,

    /** Can be dismissed by swiping in the reverse of the reading direction. */
    EndToStart,

    /** Cannot currently be dismissed. */
    Settled,
}

class SwipeToRevealBoxState {
    constructor(
        initialValue: SwipeToRevealBoxValue,
        positionalThreshold: (totalDistance: Float) -> Float,
    ) {
        this.anchoredDraggableState = AnchoredDraggableState(initialValue)
        this.positionalThreshold = positionalThreshold
    }

    internal val anchoredDraggableState: AnchoredDraggableState<SwipeToRevealBoxValue>

    internal lateinit var positionalThreshold: (Float) -> Float

    internal val useFlingBehavior: Boolean
        get() = ::positionalThreshold.isInitialized

    internal val offset: Float
        get() = anchoredDraggableState.offset

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** The current state value of the [SwipeToDismissBoxState]. */
    val currentValue: SwipeToRevealBoxValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target state. This is the closest state to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current state.
     */
    val targetValue: SwipeToRevealBoxValue
        get() = anchoredDraggableState.targetValue

    /**
     * The value the [SwipeToDismissBoxState] is currently settled at. When progressing through
     * multiple anchors, e.g. A -> B -> C, settledValue will stay the same until settled at an
     * anchor, while currentValue will update to the closest anchor.
     */
    val settledValue: SwipeToRevealBoxValue
        get() = anchoredDraggableState.settledValue

    /**
     * The direction (if any) in which the composable has been or is being dismissed.
     *
     * Use this to change the background of the [SwipeToRevealBox] if you want different actions on
     * each side.
     */
    val dismissDirection: SwipeToDismissBoxValue
        get() =
            when {
                offset == 0f || offset.isNaN() -> SwipeToDismissBoxValue.Settled
                offset > 0f -> SwipeToDismissBoxValue.StartToEnd
                else -> SwipeToDismissBoxValue.EndToStart
            }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: SwipeToRevealBoxValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Reset the component to the default position with animation and suspend until it if fully
     * reset or animation has been cancelled. This method will throw [CancellationException] if the
     * animation is interrupted
     *
     * @return the reason the reset animation ended
     */
    suspend fun reset() =
        anchoredDraggableState.animateTo(targetValue = SwipeToRevealBoxValue.Settled)

    /**
     * Dismiss the component in the given [direction], with an animation and suspend. This method
     * will throw [CancellationException] if the animation is interrupted
     *
     * @param direction The dismiss direction.
     */
    suspend fun dismiss(direction: SwipeToRevealBoxValue) {
        anchoredDraggableState.animateTo(targetValue = direction)
    }

    @Suppress("FunctionNaming")
    companion object {
        fun Saver(positionalThreshold: (totalDistance: Float) -> Float) =
            androidx.compose.runtime.saveable.Saver<SwipeToRevealBoxState, SwipeToRevealBoxValue>(
                save = { it.currentValue },
                restore = { SwipeToRevealBoxState(it, positionalThreshold) },
            )
    }
}

@Composable
fun rememberSwipeToRevealBoxState(
    initialValue: SwipeToRevealBoxValue = SwipeToRevealBoxValue.Settled,
    positionalThreshold: (totalDistance: Float) -> Float = SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToRevealBoxState = rememberSaveable(
    saver = SwipeToRevealBoxState.Saver(positionalThreshold = positionalThreshold),
) {
    SwipeToRevealBoxState(initialValue, positionalThreshold)
}
