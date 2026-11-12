package ru.matrux.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ru.matrux.app.MainActivity
import ru.matrux.app.R
import ru.matrux.app.data.EXPIRY_NOTICE_DAYS
import ru.matrux.app.data.SubscriptionStatus
import ru.matrux.app.data.TrustedClock
import java.time.LocalDate

object ExpiryNotice {
    private const val CHANNEL = "matrux_subscription"
    private const val NOTIFICATION_ID = 3
    private const val PREFS = "matrux_state"
    private const val KEY = "expiry_notice"

    fun maybeNotify(context: Context, status: SubscriptionStatus?, now: Long = TrustedClock.now()) {
        if (status == null || !status.activeAt(now)) return
        val left = status.daysLeft(now) ?: return
        if (left > EXPIRY_NOTICE_DAYS || left < 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY, null) == today) return
        prefs.edit().putString(KEY, today).apply()
        post(context, left.toInt())
    }

    private fun post(context: Context, daysLeft: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notify_subscription),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (daysLeft <= 0) {
            context.getString(R.string.notify_expiry_today)
        } else {
            context.getString(R.string.notify_expiry_days, daysLeft)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notify_expiry_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }
}
