package ru.matrux.app.store

import android.app.Activity
import android.app.Application
import android.content.Intent
import ru.matrux.app.account.AuthGateway
import ru.matrux.app.account.AuthNotConnected
import ru.matrux.app.billing.BillingGateway
import ru.matrux.app.billing.StoreNotConnected

object StoreSdk {
    fun init(app: Application) = Unit

    fun billing(app: Application): BillingGateway = StoreNotConnected()

    fun auth(app: Application): AuthGateway = AuthNotConnected()

    fun handleIntent(activity: Activity, intent: Intent, firstLaunch: Boolean) = Unit
}
