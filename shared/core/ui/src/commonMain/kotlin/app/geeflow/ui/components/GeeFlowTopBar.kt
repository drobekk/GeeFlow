package app.geeflow.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.geeflow.ui.GeeFlowInsets
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowTopBar(
    title: String,
    subtitle: String?,
    navIconClick: () -> Unit,
    navIconPainter: Painter? = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
    navIconContentDescription: String? = stringResource(Res.string.common_go_back),
    windowInsets: WindowInsets = GeeFlowInsets.top,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f

    val titleStyle = lerp(
        MaterialTheme.typography.displayMedium,
        MaterialTheme.typography.titleLarge,
        collapsedFraction,
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val subtitleStyle = MaterialTheme.typography.bodyLarge

    if (scrollBehavior != null) {
        SideEffect {
            val titleHeightPx = textMeasurer.measure(title, style = titleStyle).size.height
            val subtitleHeightPx = if (subtitle != null) {
                textMeasurer.measure(
                subtitle,
                style = subtitleStyle
            ).size.height
            } else {
                0
            }

            // The layout is dynamic but we can estimate the heights perfectly since text size is fixed and does not wrap.
            val iconHeightPx = with(density) { 48.dp.roundToPx() }
            val topPaddingPx = with(density) { 16.dp.roundToPx() }
            val bottomPaddingExpandedPx = with(density) { 32.dp.roundToPx() }
            val bottomPaddingCollapsedPx = with(density) { 16.dp.roundToPx() }
            val spacingPx = with(density) { 8.dp.roundToPx() }

            val expandedTitleYPx = topPaddingPx + iconHeightPx + spacingPx
            val expandedSubtitleYPx = expandedTitleYPx + titleHeightPx + spacingPx

            val expandedHeightPx = if (subtitle != null) {
                expandedSubtitleYPx + subtitleHeightPx + bottomPaddingExpandedPx
            } else {
                expandedTitleYPx + titleHeightPx + bottomPaddingExpandedPx
            }

            val collapsedHeightPx = iconHeightPx + topPaddingPx + bottomPaddingCollapsedPx
            val heightLimit = (collapsedHeightPx - expandedHeightPx).toFloat()

            if (scrollBehavior.state.heightOffsetLimit != heightLimit) {
                scrollBehavior.state.heightOffsetLimit = heightLimit
            }
        }
    }

    Layout(
        modifier = modifier
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = 8.dp),
        content = {
            IconButton(
                onClick = navIconClick,
                enabled = navIconPainter != null,
                modifier = Modifier.layoutId("icon"),
            ) {
                navIconPainter?.let {
                    Icon(
                        painter = navIconPainter,
                        contentDescription = navIconContentDescription,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(
                text = title,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId("title"),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .layoutId("subtitle")
                        .alpha(1f - (collapsedFraction * 2f).coerceAtMost(1f)),
                )
            }
        },
    ) { measurables, constraints ->
        val iconPlaceable = measurables.first { it.layoutId == "icon" }.measure(
            constraints.copy(minWidth = 0, minHeight = 0),
        )

        val topPadding = 16.dp.roundToPx()
        val bottomPaddingExpanded = 32.dp.roundToPx()
        val bottomPaddingCollapsed = 16.dp.roundToPx()
        val spacing = 8.dp.roundToPx()
        val endMargin = 16.dp.roundToPx()

        val expandedTitleX = 16.dp.roundToPx()
        val collapsedTitleX = iconPlaceable.width + 4.dp.roundToPx()
        val currentTitleX = lerp(expandedTitleX.toFloat(), collapsedTitleX.toFloat(), collapsedFraction).roundToInt()

        val titleMaxWidth = (constraints.maxWidth - currentTitleX - endMargin).coerceAtLeast(0)
        val titlePlaceable = measurables.first { it.layoutId == "title" }.measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = titleMaxWidth),
        )

        val subtitleMaxWidth = (constraints.maxWidth - expandedTitleX - endMargin).coerceAtLeast(0)
        val subtitlePlaceable = measurables.firstOrNull { it.layoutId == "subtitle" }?.measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = subtitleMaxWidth),
        )

        val expandedTitleY = topPadding + iconPlaceable.height + spacing
        val expandedSubtitleY = expandedTitleY + titlePlaceable.height + spacing

        val expandedHeight = if (subtitlePlaceable != null) {
            expandedSubtitleY + subtitlePlaceable.height + bottomPaddingExpanded
        } else {
            expandedTitleY + titlePlaceable.height + bottomPaddingExpanded
        }

        val collapsedTitleY = topPadding + (iconPlaceable.height - titlePlaceable.height) / 2
        val collapsedHeight = iconPlaceable.height + topPadding + bottomPaddingCollapsed

        val titleDeltaY = expandedTitleY - collapsedTitleY
        val collapsedSubtitleY = expandedSubtitleY - titleDeltaY

        val currentTitleY = lerp(expandedTitleY.toFloat(), collapsedTitleY.toFloat(), collapsedFraction).roundToInt()
        val currentSubtitleY = lerp(
            expandedSubtitleY.toFloat(),
            collapsedSubtitleY.toFloat(),
            collapsedFraction,
        ).roundToInt()
        val currentHeight = lerp(expandedHeight.toFloat(), collapsedHeight.toFloat(), collapsedFraction).roundToInt()

        layout(constraints.maxWidth, currentHeight) {
            iconPlaceable.placeRelative(0, topPadding)
            titlePlaceable.placeRelative(currentTitleX, currentTitleY)
            subtitlePlaceable?.placeRelative(expandedTitleX, currentSubtitleY)
        }
    }
}
