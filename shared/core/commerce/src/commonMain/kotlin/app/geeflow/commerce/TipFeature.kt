package app.geeflow.commerce

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface TipFeature {

    val isAvailable: Boolean

    @Composable
    fun TipSection(modifier: Modifier)
}
