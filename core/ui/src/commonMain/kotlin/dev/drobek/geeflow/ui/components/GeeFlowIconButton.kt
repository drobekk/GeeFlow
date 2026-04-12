package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GeeFlowIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 80.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = modifier.size(size),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(32.dp),
        )
    }
}
