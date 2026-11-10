package ru.palma.app.store

import android.content.Intent
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.palma.app.billing.BillingGateway
import ru.palma.app.billing.PlanOffer
import ru.palma.app.billing.PurchaseReceipt
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException
import ru.rustore.sdk.core.tasks.Task
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.AppUserId
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductType
import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult
import ru.rustore.sdk.pay.model.PurchaseStatus
import ru.rustore.sdk.pay.model.RuStorePaymentException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RuStorePayBilling(private val productIds: List<String>) : BillingGateway {
    override val available: Boolean get() = productIds.isNotEmpty()

    override suspend fun purchase(offer: PlanOffer, appUserId: String): PurchaseReceipt {
        if (offer.productId.isBlank()) throw FailureException(Failure.BillingUnavailable)
        val availability = client.getPurchaseInteractor().getPurchaseAvailability().await()
        if (availability !is PurchaseAvailabilityResult.Available) throw FailureException(Failure.BillingUnavailable)
        val params = ProductPurchaseParams(
            productId = ProductId(offer.productId),
            appUserId = AppUserId(appUserId),
        )
        val result = try {
            client.getPurchaseInteractor().purchase(params, PreferredPurchaseType.ONE_STEP).await()
        } catch (error: RuStorePaymentException.ProductPurchaseCancelled) {
            throw FailureException(Failure.Cancelled)
        } catch (error: RuStorePaymentException) {
            throw FailureException(Failure.BillingUnavailable)
        }
        return PurchaseReceipt(
            purchaseId = result.purchaseId.value,
            invoiceId = result.invoiceId.value.toString(),
            productId = result.productId.value,
        )
    }

    override suspend fun activePurchases(): List<PurchaseReceipt> {
        val purchases = client.getPurchaseInteractor()
            .getPurchases(productType = ProductType.SUBSCRIPTION)
            .await()
        return purchases
            .filter { it.status == PurchaseStatus.ACTIVE || it.status == PurchaseStatus.PAUSED }
            .filter { it.productId.value in productIds }
            .map { PurchaseReceipt(it.purchaseId.value, it.invoiceId.value.toString(), it.productId.value) }
    }

    companion object {
        private val client get() = RuStorePayClient.instance

        fun proceed(intent: Intent, firstLaunch: Boolean) {
            if (firstLaunch || intent.data != null) client.getIntentInteractor().proceedIntent(intent)
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
