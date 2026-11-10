package ru.palma.app.billing

import ru.palma.app.BuildConfig
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException

enum class Plan(val months: Int, val discountPercent: Int) {
    PRO_MONTH(1, 0),
    PRO_QUARTER(3, 15),
    PRO_HALF_YEAR(6, 30),
}

data class PlanOffer(
    val plan: Plan,
    val productId: String,
    val price: String,
    val trialDays: Int,
    val perMonth: String = "",
)

object Offers {
    fun fromBuildConfig(): List<PlanOffer> = listOf(
        PlanOffer(Plan.PRO_MONTH, BuildConfig.PRODUCT_PRO_MONTH, BuildConfig.PRICE_PRO_MONTH, BuildConfig.TRIAL_DAYS, BuildConfig.PER_MONTH_PRO_MONTH),
        PlanOffer(Plan.PRO_QUARTER, BuildConfig.PRODUCT_PRO_QUARTER, BuildConfig.PRICE_PRO_QUARTER, 0, BuildConfig.PER_MONTH_PRO_QUARTER),
        PlanOffer(Plan.PRO_HALF_YEAR, BuildConfig.PRODUCT_PRO_HALF_YEAR, BuildConfig.PRICE_PRO_HALF_YEAR, 0, BuildConfig.PER_MONTH_PRO_HALF_YEAR),
    )
}

interface BillingGateway {
    suspend fun purchase(offer: PlanOffer): String
}

class StoreNotConnected : BillingGateway {
    override suspend fun purchase(offer: PlanOffer): String = throw FailureException(Failure.BillingUnavailable)
}
