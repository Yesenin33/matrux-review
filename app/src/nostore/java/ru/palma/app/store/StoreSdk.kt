package ru.palma.app.store

import android.app.Activity
import android.app.Application
import android.content.Intent
import ru.palma.app.account.AuthGateway
import ru.palma.app.account.AuthNotConnected
import ru.palma.app.billing.BillingGateway
import ru.palma.app.billing.StoreNotConnected

object StoreSdk {
    fun init(app: Application) = Unit

    fun billing(app: Application): BillingGateway = StoreNotConnected()

    fun auth(app: Application): AuthGateway = AuthNotConnected()

    fun handleIntent(activity: Activity, intent: Intent, firstLaunch: Boolean) = Unit
}
