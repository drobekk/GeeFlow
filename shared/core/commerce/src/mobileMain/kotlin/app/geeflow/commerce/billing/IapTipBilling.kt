package app.geeflow.commerce.billing

import app.geeflow.commerce.TipBilling
import app.geeflow.commerce.TipOffer
import app.geeflow.commerce.TipResult
import app.geeflow.commerce.TipTier
import com.multiplatform.inAppPurchase.IAPManager
import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.ProductType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class IapTipBilling(
    private val prepare: (IAPManager) -> Unit = {},
) : TipBilling {

    private val iapManager = IAPManager()

    @Suppress("InjectDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val initLock = Mutex()
    private var initialized = false
    private val products = mutableMapOf<String, Product>()

    private val _completedTips = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val completedTips: Flow<Unit> = _completedTips.asSharedFlow()

    override suspend fun loadOffers(): List<TipOffer> {
        if (!ensureInitialized()) return emptyList()

        val result = iapManager.getProducts(
            productIds = TipTier.entries.map { it.productId },
            productType = ProductType.ONE_TIME_PURCHASE,
        )
        val loaded = (result as? IAPResult.Success)?.data.orEmpty()
        loaded.forEach { products[it.id] = it }

        return TipTier.entries.mapNotNull { tier ->
            products[tier.productId]?.let { product -> TipOffer(tier, product.price) }
        }
    }

    override suspend fun purchase(offer: TipOffer): TipResult {
        val product = products[offer.tier.productId] ?: return TipResult.Failed(null)
        prepare(iapManager)

        return when (val result = iapManager.launchPurchaseFlow(product)) {
            is IAPResult.Success -> TipResult.Launched
            is IAPResult.Error -> TipResult.Failed(result.message)
        }
    }

    private suspend fun ensureInitialized(): Boolean = initLock.withLock {
        if (initialized) return@withLock true

        prepare(iapManager)
        if (iapManager.initialize() !is IAPResult.Success) return@withLock false

        initialized = true
        observePurchases()
        true
    }

    private fun observePurchases() {
        scope.launch {
            iapManager.getPurchaseUpdates().collect { purchase ->
                iapManager.consumePurchase(purchase)
                _completedTips.emit(Unit)
            }
        }
    }
}
