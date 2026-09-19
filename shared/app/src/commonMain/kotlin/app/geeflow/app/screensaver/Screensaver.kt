package app.geeflow.app.screensaver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.platform.rememberFormattedDate
import app.geeflow.platform.rememberFormattedTime
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview

@Composable
internal fun Screensaver(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Screensaver(
        formattedTime = rememberFormattedTime(),
        formattedDate = rememberFormattedDate(),
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
internal fun Screensaver(
    formattedTime: String,
    formattedDate: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsedTime = remember(formattedTime) { parseScreensaverTime(formattedTime) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            event.changes.forEach { it.consume() }
                            onDismiss()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val (timeFontSize, amPmFontSize) = when {
            maxWidth < CompactWidthBreakpoint -> TimeFontCompact to AmPmStackedFontCompact
            maxWidth >= ExpandedWidthBreakpoint -> TimeFontExpanded to AmPmStackedFontExpanded
            else -> TimeFontRegular to AmPmStackedFontRegular
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = parsedTime.timeDigits,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-1).sp,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (parsedTime.hasAmPm) {
                    Spacer(modifier = Modifier.width(AmPmSpacing))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "AM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = amPmFontSize,
                                fontWeight = if (parsedTime.isAm) FontWeight.SemiBold else FontWeight.Medium,
                                lineHeight = amPmFontSize,
                            ),
                            color = if (parsedTime.isAm) {
                                MaterialTheme.colorScheme.primaryFixedDim
                            } else {
                                Color.White.copy(alpha = AmPmInactiveAlpha)
                            },
                            maxLines = 1,
                        )
                        Text(
                            text = "PM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = amPmFontSize,
                                fontWeight = if (!parsedTime.isAm) FontWeight.SemiBold else FontWeight.Medium,
                                lineHeight = amPmFontSize,
                            ),
                            color = if (!parsedTime.isAm) {
                                MaterialTheme.colorScheme.primaryFixedDim
                            } else {
                                Color.White.copy(alpha = AmPmInactiveAlpha)
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primaryFixedDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private val CompactWidthBreakpoint = 380.dp
private val ExpandedWidthBreakpoint = 600.dp
private val TimeFontCompact = 72.sp
private val TimeFontRegular = 88.sp
private val TimeFontExpanded = 120.sp
private val AmPmStackedFontCompact = 15.sp
private val AmPmStackedFontRegular = 18.sp
private val AmPmStackedFontExpanded = 24.sp
private val AmPmSpacing = 8.dp
private const val AmPmInactiveAlpha = 0.35f

private val TimeDigitsRegex = Regex("""\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")

private data class ParsedTime(
    val timeDigits: String,
    val hasAmPm: Boolean,
    val isAm: Boolean,
)

/**
 * Extracts time digits and determines 12-hour format AM/PM state from the localized
 * time string formatted by the underlying platform.
 */
private fun parseScreensaverTime(formattedTime: String): ParsedTime {
    val trimmed = formattedTime.trim()
    val digits = TimeDigitsRegex.find(trimmed)?.value ?: trimmed
    val lower = trimmed.lowercase()
    val isAm = lower.contains("am") || lower.contains("a.m.")
    val isPm = lower.contains("pm") || lower.contains("p.m.")
    val hasAmPm = isAm || isPm

    return ParsedTime(
        timeDigits = digits,
        hasAmPm = hasAmPm,
        isAm = isAm,
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Screensaver(
        formattedTime = "10:30 PM",
        formattedDate = "Saturday, 19 September",
        onDismiss = {},
    )
}
