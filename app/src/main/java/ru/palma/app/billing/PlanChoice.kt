package ru.palma.app.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Terms(val offers: List<PlanOffer>, val selected: Plan) {
    val selectedOffer: PlanOffer? get() = offers.firstOrNull { it.plan == selected }
}

class PlanChoice(offers: List<PlanOffer>, selected: Plan = Plan.PRO_MONTH) {
    private val state = MutableStateFlow(Terms(offers, selected))
    val terms: StateFlow<Terms> = state.asStateFlow()

    fun select(plan: Plan) {
        state.update { it.copy(selected = plan) }
    }

    fun applyServer(server: List<ServerPlan>) {
        state.update { it.copy(offers = Offers.merge(it.offers, server)) }
    }
}
