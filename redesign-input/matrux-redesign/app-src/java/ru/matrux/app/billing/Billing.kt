package ru.matrux.app.billing

import ru.matrux.app.BuildConfig
import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException

enum class Plan(val months: Int) {
    PRO_MONTH(1),
    PRO_QUARTER(3),
    PRO_HALF_YEAR(6),
    ;

    companion object {
        fun ofMonths(months: Int): Plan? = entries.firstOrNull { it.months == months }
    }
}

data class PlanOffer(
    val plan: Plan,
    val productId: String,
    val priceRub: Long?,
    val trialDays: Int,
    val discountPercent: Int? = null,
) {
    val perMonthRub: Long? get() = priceRub?.let { (it * 2 + plan.months) / (plan.months * 2) }
}

data class ServerPlan(val productId: String, val months: Int, val priceRub: Long, val discountPercent: Int?)

data class PurchaseReceipt(val purchaseId: String, val invoiceId: String?, val productId: String)

object Offers {
    fun fromBuildConfig(): List<PlanOffer> = listOf(
        PlanOffer(Plan.PRO_MONTH, BuildConfig.PRODUCT_PRO_MONTH, rubles(BuildConfig.PRICE_PRO_MONTH), BuildConfig.TRIAL_DAYS),
        PlanOffer(Plan.PRO_QUARTER, BuildConfig.PRODUCT_PRO_QUARTER, rubles(BuildConfig.PRICE_PRO_QUARTER), 0),
        PlanOffer(Plan.PRO_HALF_YEAR, BuildConfig.PRODUCT_PRO_HALF_YEAR, rubles(BuildConfig.PRICE_PRO_HALF_YEAR), 0),
    )

    fun rubles(price: String): Long? = price.filter(Char::isDigit).toLongOrNull()

    fun merge(local: List<PlanOffer>, server: List<ServerPlan>): List<PlanOffer> = local.map { offer ->
        val match = server.firstOrNull { it.months == offer.plan.months }
            ?: return@map offer
        offer.copy(
            productId = match.productId.ifBlank { offer.productId },
            priceRub = match.priceRub,
            discountPercent = match.discountPercent?.takeIf { it > 0 },
        )
    }

    fun productIds(offers: List<PlanOffer>): List<String> = offers.map { it.productId }.filter { it.isNotBlank() }
}

interface BillingGateway {
    val available: Boolean
    suspend fun purchase(offer: PlanOffer, appUserId: String): PurchaseReceipt
    suspend fun activePurchases(): List<PurchaseReceipt>
}

class StoreNotConnected : BillingGateway {
    override val available = false
    override suspend fun purchase(offer: PlanOffer, appUserId: String): PurchaseReceipt =
        throw FailureException(Failure.BillingUnavailable)
    override suspend fun activePurchases(): List<PurchaseReceipt> = emptyList()
}
