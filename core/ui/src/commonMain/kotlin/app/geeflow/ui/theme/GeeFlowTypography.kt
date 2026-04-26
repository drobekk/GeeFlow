package app.geeflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import geeflow.core.ui.generated.resources.Bitter_Regular
import geeflow.core.ui.generated.resources.Bitter_SemiBold
import geeflow.core.ui.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
internal fun geeFlowTypography(): Typography {
    val serifFont = FontFamily(
        Font(Res.font.Bitter_Regular, FontWeight.Normal),
        Font(Res.font.Bitter_SemiBold, FontWeight.Bold),
    )

    return with(MaterialTheme.typography) {
        MaterialTheme.typography.copy(
            displayLarge = displayLarge.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            displayMedium = displayMedium.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            displaySmall = displaySmall.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            headlineLarge = headlineLarge.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            headlineMedium = headlineMedium.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            headlineSmall = headlineSmall.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            titleLarge = titleLarge.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            titleMedium = titleMedium.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
            titleSmall = titleSmall.copy(fontFamily = serifFont, fontWeight = FontWeight.Bold),
        )
    }
}
