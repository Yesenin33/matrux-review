package ru.matrux.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

enum class Tier { FREE, PRO }

const val EXPIRY_NOTICE_DAYS = 3

data class SubscriptionStatus(
    val tier: Tier,
    val expiresAt: Instant?,
    val checkedAt: Instant?,
) {
    val paid: Boolean get() = tier != Tier.FREE

    fun activeAt(nowMillis: Long): Boolean {
        if (!paid) return false
        val expires = expiresAt ?: return true
        return nowMillis <= expires.toEpochMilli()
    }

    fun endsAt(nowMillis: Long): Boolean = paid && expiresAt != null && !activeAt(nowMillis)

    fun daysLeft(nowMillis: Long): Long? {
        val expires = expiresAt ?: return null
        val left = expires.toEpochMilli() - nowMillis
        if (left <= 0) return 0
        return (left + 86_399_999L) / 86_400_000L
    }

    companion object {
        val FREE = SubscriptionStatus(Tier.FREE, null, null)

        fun parse(body: String): SubscriptionStatus {
            val json = JSONObject(body)
            return SubscriptionStatus(
                tier = when (json.getString("tier")) {
                    "pro" -> Tier.PRO
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
