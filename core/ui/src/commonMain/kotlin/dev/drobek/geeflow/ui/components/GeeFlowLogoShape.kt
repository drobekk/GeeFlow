package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.shape.GenericShape

val GeeFlowLogoShape = GenericShape { size, _ ->
    val scaleX = size.width / 1024f
    val scaleY = size.height / 1024f

    moveTo(512f * scaleX, 0f * scaleY)
    cubicTo(
        512f * scaleX, 0f * scaleY,
        568.3f * scaleX, 67.6f * scaleY,
        664f * scaleX, 155f * scaleY
    )
    cubicTo(
        781.2f * scaleX, 262f * scaleY,
        942.1f * scaleX, 406.8f * scaleY,
        942.1f * scaleX, 593.9f * scaleY
    )
    cubicTo(
        942.1f * scaleX, 831.3f * scaleY,
        749.4f * scaleX, 1024f * scaleY,
        512f * scaleX, 1024f * scaleY
    )
    cubicTo(
        274.6f * scaleX, 1024f * scaleY,
        81.9f * scaleX, 831.3f * scaleY,
        81.9f * scaleX, 593.9f * scaleY
    )
    cubicTo(
        81.9f * scaleX, 411.4f * scaleY,
        242.1f * scaleX, 259f * scaleY,
        356.9f * scaleX, 152.1f * scaleY
    )
    cubicTo(
        455.9f * scaleX, 59.8f * scaleY,
        512f * scaleX, 0f * scaleY,
        512f * scaleX, 0f * scaleY
    )

    close()
}
