package hidden.the.projectx.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import hidden.the.projectx.MainActivity
import hidden.the.projectx.R

class LocationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    companion object {
        const val CHANNEL_ID = "PROJECTX_LOCATION_CHANNEL"
        const val NOTIF_ID = 1001
        
        const val ACTION_START_FAKE = "ACTION_START_FAKE"
        const val ACTION_STOP_FAKE = "ACTION_STOP_FAKE"
        const val ACTION_STATE_CHANGED = "hidden.the.projectx.STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "is_running"

        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FAKE -> {
                val autoStopMillis = intent.getLongExtra("AUTO_STOP_DELAY", 0L)
                startFakeLocationService(autoStopMillis)
            }
            ACTION_STOP_FAKE -> {
                stopFakeLocationService()
            }
        }
        return START_STICKY
    }

    private fun startFakeLocationService(autoStopDelay: Long) {
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIF_ID, notification)

        isServiceRunning = true
        broadcastStatus(true)

        // Batalkan timer lama jika ada
        autoStopRunnable?.let { handler.removeCallbacks(it) }

        // Setup Auto Stop Timer jika durasi > 0
        if (autoStopDelay > 0) {
            autoStopRunnable = Runnable {
                // Trigger Auto Stop saat timer habis
                stopFakeLocationService()
            }
            handler.postDelayed(autoStopRunnable!!, autoStopDelay)
        }
    }

    private fun stopFakeLocationService() {
        // Hentikan timer jika di-stop manual sebelum timer habis
        autoStopRunnable?.let { handler.removeCallbacks(it) }
        autoStopRunnable = null

        isServiceRunning = false
        broadcastStatus(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun broadcastStatus(isRunning: Boolean) {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_RUNNING, isRunning)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spoofing Lokasi Aktif")
            .setContentText("Project-X sedang memposting lokasi palsu...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Pastikan drawable icon ada
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        autoStopRunnable?.let { handler.removeCallbacks(it) }
        isServiceRunning = false
        broadcastStatus(false)
        super.onDestroy()
    }
}
