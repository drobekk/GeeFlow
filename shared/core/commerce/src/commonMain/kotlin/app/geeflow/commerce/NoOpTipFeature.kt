package app.geeflow.commerce

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object NoOpTipFeature : TipFeature {

    override val isAvailable: Boolean = false

    @Composable
    override fun TipSection(modifier: Modifier) = Unit
}
