package app.geeflow.commerce.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.commerce.TipBilling
import app.geeflow.commerce.TipFeature
import app.geeflow.commerce.resources.Res
import app.geeflow.commerce.resources.tip_button
import app.geeflow.commerce.resources.tip_section_prompt
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

internal class StoreTipFeature(
    private val billing: TipBilling,
) : TipFeature {

    override val isAvailable: Boolean = true

    @Composable
    override fun TipSection(modifier: Modifier) {
        TipSectionContent(billing = billing, modifier = modifier)
    }
}

@Composable
private fun TipSectionContent(
    billing: TipBilling,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<TipDialogState?>(null) }

    fun loadOffers() {
        dialogState = TipDialogState.Loading
        scope.launch {
            val offers = billing.loadOffers()
            dialogState = if (offers.isEmpty()) TipDialogState.Error else TipDialogState.Offers(offers)
        }
    }

    LaunchedEffect(billing) {
        billing.completedTips.collect { dialogState = null }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.tip_section_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(8.dp)
        Button(
            onClick = ::loadOffers,
        ) {
            Icon(
                imageVector = Icons.Outlined.Coffee,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(8.dp)
            Text(stringResource(Res.string.tip_button))
        }
    }

    dialogState?.let { state ->
        TipDialog(
            state = state,
            onOfferSelected = { offer ->
                dialogState = null
                scope.launch { billing.purchase(offer) }
            },
            onRetry = ::loadOffers,
            onDismiss = { dialogState = null },
        )
    }
}
