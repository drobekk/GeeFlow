package dev.drobek.geeflow.presentation.feature.device.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_apply
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsApplyFab(
    loading: Boolean,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { it } + fadeOut(),
    modifier = modifier
) {
    FloatingActionButton(
        modifier = Modifier.padding(
            horizontal = GeeFlowTheme.spacing.fabHorizontal,
            vertical = GeeFlowTheme.spacing.fabVertical
        ),
        onClick = { if (!loading) onClick() }
    ) {
        AnimatedContent(
            targetState = loading,
            label = "Apply button"
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(horizontal = 24.dp).size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Check),
                        contentDescription = null
                    )
                    HorizontalSpacer(8.dp)
                    Text(text = stringResource(Res.string.common_apply))
                }
            }
        }
    }
}

internal val SettingsApplyFabPadding = 88.dp
