package dev.drobek.geeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_off
import geeflow.composeapp.generated.resources.common_on
import org.jetbrains.compose.resources.stringResource

@Composable
fun GeeFlowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)

    val offBorderColor by animateColorAsState(
        if (!checked) MaterialTheme.colorScheme.primary else Color.Transparent
    )
    val offTextColor by animateColorAsState(
        if (!checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
    val onBackgroundColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    )
    val onTextColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .alpha(if (enabled) 1f else 0.38f)
    ) {
        Text(
            text = stringResource(Res.string.common_off),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape)
                .clickable(enabled = enabled) { onCheckedChange(false) }
                .border(2.dp, offBorderColor, shape)
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (!checked) FontWeight.Bold else FontWeight.Normal,
            color = offTextColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(Res.string.common_on),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape)
                .clickable(enabled = enabled) { onCheckedChange(true) }
                .background(onBackgroundColor)
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color = onTextColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
private fun PreviewLight() = GeeFlowTheme(false) {
    Row(Modifier.width(200.dp)) {
        GeeFlowSwitch(checked = false, onCheckedChange = {}, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        GeeFlowSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowTheme(true) {
    Row(Modifier.width(200.dp)) {
        GeeFlowSwitch(checked = false, onCheckedChange = {}, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        GeeFlowSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
    }
}
