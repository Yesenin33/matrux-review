package ru.palma.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

enum class Tier { FREE, PRO, LIFETIME }

data class SubscriptionStatus(
    val tier: Tier,
    val expiresAt: Instant?,
    val checkedAt: Instant?,
) {
    val paid: Boolean get() = tier != Tier.FREE

    companion object {
        val FREE = SubscriptionStatus(Tier.FREE, null, null)

        fun parse(body: String): SubscriptionStatus {
            val json = JSONObject(body)
            return SubscriptionStatus(
                tier = when (json.getString("tier")) {
                    "pro" -> Tier.PRO
                    "lifetime" -> Tier.LIFETIME
                    else -> Tier.FREE
                },
                expiresAt = json.optStringOrNull("expires_at")?.let(::instant),
                checkedAt = json.optStringOrNull("checked_at")?.let(::instant),
            )
        }

        fun instant(value: String): Instant? =
            runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
                ?: runCatching { Instant.parse(value) }.getOrNull()
    }
}

enum class PurchaseState { ACTIVE, PAUSED, TERMINATED }

data class VerifyResult(val state: PurchaseState, val status: SubscriptionStatus)
