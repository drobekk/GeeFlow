package app.geeflow.commerce.ui

import app.geeflow.commerce.TipOffer

internal sealed interface TipDialogState {
    data object Loading : TipDialogState
    data class Offers(val offers: List<TipOffer>) : TipDialogState
    data object Error : TipDialogState
}
