package ru.matrux.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.matrux.app.R
import ru.matrux.app.graph
import ru.matrux.app.session.SessionState
import ru.matrux.app.shell.LaunchState

class SessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null
    private var lock: PowerManager.WakeLock? = null

    private fun holdAwake() {
        if (lock != null) return
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "matrux:session").apply { setReferenceCounted(false); acquire() }
    }

    private fun releaseAwake() {
        lock?.let { if (it.isHeld) it.release() }
        lock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannel(this)
        val name = graph.local.installed?.name.orEmpty()
        val notification = Notifications.builder(this)
            .setContentTitle(getString(R.string.notify_session, name))
            .setContentText(getString(R.string.notify_session_detail))
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, Notifications.SESSION_ID, notification, type)
        holdAwake()
        if (intent == null) {
            finish()
            return START_NOT_STICKY
        }
        if (watcher == null) {
            watcher = scope.launch {
                graph.session.state.collect { state ->
                    val failed = state is SessionState.Launching && state.launch is LaunchState.Failed
                    if (state is SessionState.Stopped || state is SessionState.Lost || failed) finish()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun finish() {
        releaseAwake()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseAwake()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SessionService::class.java).setAction("session"))
        }
    }
}
