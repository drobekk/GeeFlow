package app.geeflow.commerce

import kotlinx.coroutines.flow.Flow

/**
 * Store facade used by the UI. Purchases complete asynchronously: [purchase] only reports whether
 * the native flow was launched, while a finished and consumed purchase shows up on [completedTips].
 */
interface TipBilling {

    val completedTips: Flow<Unit>

    suspend fun loadOffers(): List<TipOffer>

    suspend fun purchase(offer: TipOffer): TipResult
}

sealed interface TipResult {
    data object Launched : TipResult
    data class Failed(val message: String?) : TipResult
}
