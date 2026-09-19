package hidden.the.projectx.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import hidden.the.projectx.MainActivity

class LocationService : Service() {

    companion object {
        const val CHANNEL_ID = "PROJECTX_LOCATION_CHANNEL"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PROJECT-X Active")
            .setContentText("Fake location running...")
            .setSmallIcon(applicationInfo.icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val broadcastIntent = Intent("hidden.the.projectx.LOCATION_STATUS_UPDATE")
        broadcastIntent.putExtra("is_running", true)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val broadcastIntent = Intent("hidden.the.projectx.LOCATION_STATUS_UPDATE")
        broadcastIntent.putExtra("is_running", false)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
