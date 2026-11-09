package ru.palma.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.palma.app.R
import ru.palma.app.data.ManifestParser
import ru.palma.app.graph
import ru.palma.app.install.InstallState

class InstallService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannel(this)
        val notification = Notifications.builder(this).setContentTitle(getString(R.string.notify_channel)).build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, Notifications.INSTALL_ID, notification, type)
        if (intent == null) resumePending()
        if (watcher == null) watcher = scope.launch { watch() }
        return START_STICKY
    }

    private fun resumePending() {
        val graph = graph
        val pending = graph.local.pendingInstall ?: return
        val cached = graph.local.manifestCache ?: return
        val image = runCatching { ManifestParser.parse(cached.first) }.getOrNull()?.images?.firstOrNull { it.id == pending } ?: return
        graph.installs.request(image)
    }

    private suspend fun watch() {
        val manager = NotificationManagerCompat.from(this)
        graph.installs.state.collect { state ->
            when (state) {
                is InstallState.Running -> {
                    val notification = Notifications.builder(this)
                        .setContentTitle(getString(R.string.notify_installing, state.image.name))
                        .setContentText("${state.percent}%")
                        .setProgress(PROGRESS_MAX, (state.fraction * PROGRESS_MAX).toInt(), false)
                        .build()
                    if (manager.areNotificationsEnabled()) {
                        runCatching { manager.notify(Notifications.INSTALL_ID, notification) }
                    }
                }
                InstallState.Idle -> if (graph.local.pendingInstall == null) finish()
                else -> finish()
            }
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PROGRESS_MAX = 1000

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, InstallService::class.java).setAction("install"))
        }
    }
}
