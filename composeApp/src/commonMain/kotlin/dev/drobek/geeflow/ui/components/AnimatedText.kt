package dev.drobek.geeflow.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Row(
        modifier = modifier.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.mapIndexed { index, c -> AnimatedTextDigit(c, text, index) }
            .forEach { digit ->
                AnimatedContent(
                    targetState = digit,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { -it } togetherWith slideOutVertically { it }
                        } else {
                            slideInVertically { it } togetherWith slideOutVertically { -it }
                        }
                    }
                ) { d ->
                    Text(
                        text = "${d.char}",
                        style = style,
                        color = color,
                        fontWeight = fontWeight,
                        textAlign = textAlign,
                    )
                }
            }
    }
}

private data class AnimatedTextDigit(val char: Char, val fullText: String, val place: Int) {
    override fun equals(other: Any?) = when (other) {
        is AnimatedTextDigit -> char == other.char
        else -> super.equals(other)
    }

    override fun hashCode() = char.hashCode()
}

private operator fun AnimatedTextDigit.compareTo(other: AnimatedTextDigit): Int =
    fullText.compareTo(other.fullText)
