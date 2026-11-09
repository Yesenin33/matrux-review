package ru.palma.app.billing

import ru.palma.app.BuildConfig
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException

enum class Plan { PRO_MONTH, PRO_YEAR, LIFETIME }

data class PlanOffer(
    val plan: Plan,
    val productId: String,
    val price: String,
    val trialDays: Int,
)

object Offers {
    fun fromBuildConfig(): List<PlanOffer> = listOf(
        PlanOffer(Plan.PRO_MONTH, BuildConfig.PRODUCT_PRO_MONTH, BuildConfig.PRICE_PRO_MONTH, BuildConfig.TRIAL_DAYS),
        PlanOffer(Plan.PRO_YEAR, BuildConfig.PRODUCT_PRO_YEAR, BuildConfig.PRICE_PRO_YEAR, BuildConfig.TRIAL_DAYS),
        PlanOffer(Plan.LIFETIME, BuildConfig.PRODUCT_LIFETIME, BuildConfig.PRICE_LIFETIME, 0),
    )
}

interface BillingGateway {
    suspend fun purchase(offer: PlanOffer): String
}

class StoreNotConnected : BillingGateway {
    override suspend fun purchase(offer: PlanOffer): String = throw FailureException(Failure.BillingUnavailable)
}
