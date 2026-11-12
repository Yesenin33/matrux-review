package ru.matrux.app.billing

import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException
import ru.matrux.app.data.Failures

class Checkout<T>(
    private val billing: BillingGateway,
    private val accountId: () -> String?,
    private val signIn: suspend () -> Unit,
    private val verify: suspend (PurchaseReceipt) -> T,
) {
    suspend fun buy(offer: PlanOffer): T {
        val buyer = signedInAccount() ?: throw FailureException(Failure.SignInRejected)
        return verify(billing.purchase(offer, buyer))
    }

    suspend fun attempt(offer: PlanOffer, onBought: (T) -> Unit): Failure? = try {
        onBought(buy(offer))
        null
    } catch (error: FailureException) {
        error.failure.takeIf { it != Failure.Cancelled }
    } catch (error: Throwable) {
        Failures.fromThrowable(error)
    }

    private suspend fun signedInAccount(): String? {
        accountId()?.takeIf { it.isNotBlank() }?.let { return it }
        signIn()
        return accountId()?.takeIf { it.isNotBlank() }
    }
}
