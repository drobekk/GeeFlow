package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Person
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper

@Composable
fun GeeFlowUserAvatar(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // TODO Load avatar from uri
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.Person),
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.outlineVariant,
            contentDescription = null,
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    GeeFlowUserAvatar(modifier = Modifier.requiredSize(56.dp))
}
