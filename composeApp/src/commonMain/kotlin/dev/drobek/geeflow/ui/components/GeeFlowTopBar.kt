package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.VerticalSpacer
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

@Composable
fun GeeFlowTopBar(
    title: String,
    subtitle: String?,
    navIconPainter: Painter? = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
    nacIconContentDescription: String? = stringResource(Res.string.common_go_back),
    navIconClick: () -> Unit,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier
        .windowInsetsPadding(windowInsets)
        .padding(16.dp)
        .fillMaxWidth()
) {
    IconButton(onClick = navIconClick, enabled = navIconPainter != null) {
        navIconPainter?.let {
            Icon(
                painter = navIconPainter,
                contentDescription = nacIconContentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    VerticalSpacer(8.dp)
    Text(
        text = title,
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    subtitle?.let {
        VerticalSpacer(8.dp)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    VerticalSpacer(32.dp)
}
