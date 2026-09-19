package app.geeflow.commerce.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.commerce.TipOffer
import app.geeflow.commerce.TipTier
import app.geeflow.commerce.resources.Res
import app.geeflow.commerce.resources.tip_dialog_error
import app.geeflow.commerce.resources.tip_dialog_retry
import app.geeflow.commerce.resources.tip_dialog_title
import app.geeflow.commerce.resources.tip_tier_beans
import app.geeflow.commerce.resources.tip_tier_beans_description
import app.geeflow.commerce.resources.tip_tier_cappuccino
import app.geeflow.commerce.resources.tip_tier_cappuccino_description
import app.geeflow.commerce.resources.tip_tier_espresso
import app.geeflow.commerce.resources.tip_tier_espresso_description
import app.geeflow.commerce.resources.tip_tier_year_of_coffee
import app.geeflow.commerce.resources.tip_tier_year_of_coffee_description
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowListItem
import app.geeflow.ui.components.VerticalSpacer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TipDialog(
    state: TipDialogState,
    onOfferSelected: (TipOffer) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.tip_dialog_title),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(8.dp)
            when (state) {
                is TipDialogState.Loading -> LoadingContent()
                is TipDialogState.Offers -> OffersContent(state.offers, onOfferSelected)
                is TipDialogState.Error -> ErrorContent(onRetry)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun OffersContent(
    offers: List<TipOffer>,
    onOfferSelected: (TipOffer) -> Unit,
) {
    Column {
        offers.forEach { offer ->
            GeeFlowListItem(
                title = stringResource(offer.tier.titleRes),
                subtitle = stringResource(offer.tier.descriptionRes),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                trailingContent = {
                    Text(
                        text = offer.formattedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOfferSelected(offer) },
            )
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(Res.string.tip_dialog_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(Res.string.tip_dialog_retry))
        }
    }
}

private val TipTier.titleRes: StringResource
    get() = when (this) {
        TipTier.Espresso -> Res.string.tip_tier_espresso
        TipTier.Cappuccino -> Res.string.tip_tier_cappuccino
        TipTier.Beans -> Res.string.tip_tier_beans
        TipTier.YearOfCoffee -> Res.string.tip_tier_year_of_coffee
    }

private val TipTier.descriptionRes: StringResource
    get() = when (this) {
        TipTier.Espresso -> Res.string.tip_tier_espresso_description
        TipTier.Cappuccino -> Res.string.tip_tier_cappuccino_description
        TipTier.Beans -> Res.string.tip_tier_beans_description
        TipTier.YearOfCoffee -> Res.string.tip_tier_year_of_coffee_description
    }
