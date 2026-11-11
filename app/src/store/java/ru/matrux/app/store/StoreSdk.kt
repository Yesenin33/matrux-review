package ru.matrux.app.store

import android.app.Activity
import android.app.Application
import android.content.Intent
import ru.matrux.app.BuildConfig
import ru.matrux.app.account.AuthGateway
import ru.matrux.app.account.AuthNotConnected
import ru.matrux.app.billing.BillingGateway
import ru.matrux.app.billing.Offers
import ru.matrux.app.billing.StoreNotConnected

object StoreSdk {
    private val billingReady get() = BuildConfig.RUSTORE_APP_ID.isNotBlank()
    private val authReady get() = BuildConfig.VKID_CLIENT_ID.isNotBlank()

    fun init(app: Application) {
        if (authReady) runCatching { VkIdAuth.init(app) }
    }

    fun billing(app: Application): BillingGateway =
        if (billingReady) RuStorePayBilling(Offers.productIds(Offers.fromBuildConfig())) else StoreNotConnected()

    fun auth(app: Application): AuthGateway = if (authReady) VkIdAuth() else AuthNotConnected()

    fun handleIntent(activity: Activity, intent: Intent, firstLaunch: Boolean) {
        if (billingReady) runCatching { RuStorePayBilling.proceed(intent, firstLaunch) }
    }
}
