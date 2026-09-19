package app.geeflow.commerce

/**
 * The three tip levels. Every tier is a consumable one-time product so it can be bought again;
 * the ids must match the products configured in Google Play Console and App Store Connect.
 */
enum class TipTier(val productId: String) {
    Espresso("geeflow_tip_tier1"),
    Cappuccino("geeflow_tip_tier2"),
    Beans("geeflow_tip_tier3"),
    YearOfCoffee("geeflow_tip_tier4"),
}

/** A tier paired with the localized price the store reported for it. */
data class TipOffer(
    val tier: TipTier,
    val formattedPrice: String,
)
