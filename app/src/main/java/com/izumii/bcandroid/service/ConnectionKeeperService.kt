package com.izumii.bcandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.izumii.bcandroid.MainActivity
import com.izumii.bcandroid.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service that prevents Android and OEM battery managers (MIUI / HyperOS, OneUI)
 * from freezing CPU threads or closing active WebSocket room connections when minimized.
 */
class ConnectionKeeperService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var pulseJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val CHANNEL_ID = "bc_connection_keeper"
        private const val NOTIFICATION_ID = 1001

        var onPulseListener: (() -> Unit)? = null

        fun buildNotification(context: Context, unreadCount: Int = 0): Notification {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val isDiscreet = context.packageName.contains("discreet")
            val baseTitle = if (isDiscreet) "BC" else "Bondage Club"
            val title = if (unreadCount > 0) "$baseTitle ($unreadCount)" else baseTitle
            val text = if (unreadCount > 0) {
                if (isDiscreet) "$unreadCount new message${if (unreadCount > 1) "s" else ""}" else "$unreadCount new chat message${if (unreadCount > 1) "s" else ""}"
            } else {
                if (isDiscreet) "Active session" else "Active room session running in background"
            }
            val icon = if (isDiscreet) R.drawable.ic_transparent else R.drawable.ic_rope_logo

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setOngoing(true)
                .setPriority(if (unreadCount > 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .apply {
                    if (unreadCount > 0) setNumber(unreadCount)
                }
                .build()
        }

        fun updateBadgeCount(context: Context, count: Int) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            try {
                manager.notify(NOTIFICATION_ID, buildNotification(context, count))
            } catch (ignored: Exception) {
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, ConnectionKeeperService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (ignored: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ConnectionKeeperService::class.java))
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BCAndroid:ConnectionKeeperWakeLock")?.apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24-hour ceiling to support long background sessions
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(this, 0)
        startForeground(NOTIFICATION_ID, notification)

        // Trigger active socket keepalive pulse every 20 seconds to prevent NAT/carrier timeout
        pulseJob?.cancel()
        pulseJob = serviceScope.launch {
            while (isActive) {
                delay(20_000L)
                try {
                    onPulseListener?.invoke()
                } catch (ignored: Exception) {
                }
                try {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        val conn = java.net.URL("https://www.google.com/generate_204").openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        conn.requestMethod = "HEAD"
                        conn.responseCode
                        conn.disconnect()
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Room Session Keep-Alive",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains room connection while the app is in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            pulseJob?.cancel()
            pulseJob = null
            onPulseListener = null
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
