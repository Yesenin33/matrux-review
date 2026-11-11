package ru.matrux.app.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException

class CheckoutTest {
    private val offer = PlanOffer(Plan.PRO_MONTH, "palma_pro_month", 199, 7)
    private val steps = mutableListOf<String>()
    private var account: String? = null

    private val billing = object : BillingGateway {
        override val available = true
        override suspend fun purchase(offer: PlanOffer, appUserId: String): PurchaseReceipt {
            steps += "purchase:$appUserId"
            return PurchaseReceipt("p-1", "77", offer.productId)
        }
        override suspend fun activePurchases(): List<PurchaseReceipt> = emptyList()
    }

    private fun checkout(signIn: suspend () -> Unit) = Checkout(
        billing = billing,
        accountId = { account },
        signIn = { steps += "signIn"; signIn() },
        verify = { receipt -> steps += "verify:${receipt.purchaseId}"; receipt },
    )

    private suspend fun failureOf(block: suspend () -> Unit): Failure? = try {
        block()
        null
    } catch (error: FailureException) {
        error.failure
    }

    @Test
    fun signsInFirstThenBuysWithSameTap() = runTest {
        var bought: PurchaseReceipt? = null
        val failure = checkout { account = "12" }.attempt(offer) { bought = it }
        assertNull(failure)
        assertEquals(listOf("signIn", "purchase:12", "verify:p-1"), steps)
        assertEquals("p-1", bought?.purchaseId)
    }

    @Test
    fun cancelledSignInBuysNothingAndShowsNoError() = runTest {
        val failure = checkout { throw FailureException(Failure.Cancelled) }.attempt(offer) { fail("nothing bought") }
        assertNull(failure)
        assertEquals(listOf("signIn"), steps)
    }

    @Test
    fun unavailableSignInBuysNothingAndShowsSignInError() = runTest {
        val failure = checkout { throw FailureException(Failure.SignInUnavailable) }.attempt(offer) { fail("nothing bought") }
        assertEquals(Failure.SignInUnavailable, failure)
        assertEquals(listOf("signIn"), steps)
    }

    @Test
    fun signedInBuysImmediately() = runTest {
        account = "12"
        val failure = checkout { fail("sign-in must not run") }.attempt(offer) { }
        assertNull(failure)
        assertEquals(listOf("purchase:12", "verify:p-1"), steps)
    }

    @Test
    fun signInWithoutAccountBuysNothing() = runTest {
        val failure = failureOf { checkout { }.buy(offer) }
        assertEquals(Failure.SignInRejected, failure)
        assertTrue(steps.none { it.startsWith("purchase") })
    }

    @Test
    fun blankAccountIsNotAccount() = runTest {
        account = ""
        checkout { account = "12" }.buy(offer)
        assertEquals(listOf("signIn", "purchase:12", "verify:p-1"), steps)
    }
}
