package ru.matrux.app.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PurchaseGate(
    private val signedIn: () -> Boolean,
    private val purchase: (PlanOffer) -> Unit,
) {
    private val openState = MutableStateFlow(false)
    val open: StateFlow<Boolean> = openState.asStateFlow()
    private var pending: PlanOffer? = null

    fun buy(offer: PlanOffer) {
        if (signedIn()) {
            purchase(offer)
        } else {
            pending = offer
            openState.value = true
        }
    }

    fun show() {
        openState.value = true
    }

    fun onSignedIn() {
        openState.value = false
        val offer = pending ?: return
        pending = null
        purchase(offer)
    }

    fun dismiss() {
        openState.value = false
        pending = null
    }
}
