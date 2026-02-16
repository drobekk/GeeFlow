package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.isExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme

@Composable
fun AdaptiveColumnRow(
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    firstColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    secondColor: Color = MaterialTheme.colorScheme.surface,
    isExpanded: Boolean = isExpanded(),
    firstAlignment: Alignment = Alignment.TopStart,
    secondAlignment: Alignment = Alignment.TopStart
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(secondColor)
            .then(modifier)
    ) {
        if (isExpanded) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(IntrinsicSize.Max)
                        .background(firstColor),
                    contentAlignment = firstAlignment
                ) {
                    first()
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = secondAlignment
                ) {
                    second()
                    WaveDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = firstColor,
                        orientation = WaveOrientation.Vertical
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(firstColor),
                contentAlignment = firstAlignment
            ) {
                first()
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = secondAlignment
            ) {
                second()
                WaveDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = firstColor,
                    orientation = WaveOrientation.Horizontal
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
@GeeFlowPreview
private fun AdaptiveColumnRowPreview() = GeeFlowTheme {
    AdaptiveColumnRow(
        isExpanded = isExpanded(),
        first = {
            Text(
                text = "First section content",
                color = Color.Black,
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
            )
        },
        second = {
            Text(
                text = "Second section will fill the rest of the screen even if content is short",
                color = Color.Black
            )
        }
    )
}
