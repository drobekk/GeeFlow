package app.geeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_off
import geeflow.shared.core.ui.generated.resources.common_on
import org.jetbrains.compose.resources.stringResource

val DefaultControlWidth: Dp = 160.dp

@Composable
fun GeeFlowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier.width(DefaultControlWidth),
    enabled: Boolean = true,
) {
    GeeFlowSwitch(
        checked = checked,
        value = stringResource(Res.string.common_on),
        onOffClick = { onCheckedChange(false) },
        onValueClick = { onCheckedChange(true) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun GeeFlowSwitch(
    checked: Boolean,
    value: String,
    onOffClick: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier.width(DefaultControlWidth),
    enabled: Boolean = true,
) {
    val shape = MaterialTheme.shapes.large
    val primaryColor = MaterialTheme.colorScheme.primary

    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "SwitchFraction",
    )

    val offTextColor by animateColorAsState(
        targetValue = if (!checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "OffTextColor",
    )
    val onTextColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "OnTextColor",
    )

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .alpha(if (enabled) 1f else DisabledAlpha),
    ) {
        // Sliding Indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(IndicatorWidthFraction)
                .graphicsLayer {
                    translationX = size.width * fraction
                }
                .clip(shape)
                .background(primaryColor.copy(alpha = fraction))
                .border(2.dp, primaryColor, shape),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.common_off),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .clickable(enabled = enabled, onClick = onOffClick)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = offTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .clickable(enabled = enabled, onClick = onValueClick)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = onTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GeeFlowValueSwitch(
    checked: Boolean,
    value: String,
    onOffClick: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier.width(DefaultControlWidth),
    enabled: Boolean = true,
) = GeeFlowSwitch(
    checked = checked,
    value = value,
    onOffClick = onOffClick,
    onValueClick = onValueClick,
    modifier = modifier,
    enabled = enabled,
)

private const val DisabledAlpha = 0.38f
private const val IndicatorWidthFraction = 0.5f

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    Column(Modifier.width(320.dp)) {
        Row {
            GeeFlowSwitch(checked = false, onCheckedChange = {}, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            GeeFlowSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row {
            GeeFlowSwitch(
                checked = false,
                value = "93°C",
                onOffClick = {},
                onValueClick = {},
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            GeeFlowSwitch(
                checked = true,
                value = "93°C",
                onOffClick = {},
                onValueClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
