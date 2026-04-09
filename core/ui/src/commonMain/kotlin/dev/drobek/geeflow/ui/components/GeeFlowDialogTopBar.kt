package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
fun GeeFlowDialogTopBar(
    title: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        )
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.common_close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
