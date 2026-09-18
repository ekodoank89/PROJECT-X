package hidden.the.projectx.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import hidden.the.projectx.core.ConfigPusher
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.core.Targets

class GojekNotifListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sbnNotNull = sbn ?: return

        // 1. Cek apakah notifikasi berasal dari Gojek Driver
        if (sbnNotNull.packageName == "com.gojek.partner") {
            val extras = sbnNotNull.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

            val targetString = "Anda dapat trip baru!"

            // 2. Cek apakah mengandung string target di Judul atau Isi Notifikasi
            if (title.contains(targetString, ignoreCase = true) ||
                text.contains(targetString, ignoreCase = true) ||
                bigText.contains(targetString, ignoreCase = true)
            ) {
                Log.d("PROJECT-X", "Trip Gojek Ditemukan! Mematikan Fake GPS Gojek...")
                stopGojekSpoofing()
            }
        }
    }

    private fun stopGojekSpoofing() {
        val prefs = Prefs(this)
        val gojekTarget = Targets.GOJEK

        // Jika spoofing Gojek sedang aktif, matikan
        if (prefs.isSpoofActive(gojekTarget.id)) {
            prefs.setSpoofActive(gojekTarget.id, false)
            
            // Push konfigurasi terbaru ke target
            val pusher = ConfigPusher(this)
            pusher.push(gojekTarget)
            
            Log.d("PROJECT-X", "Fake GPS Gojek berhasil di-STOP otomatis.")
        }
    }

private fun stopGojekSpoofing() {
    val prefs = Prefs(this)
    val gojekTarget = Targets.GOJEK

    if (prefs.isSpoofActive(gojekTarget.id)) {
        prefs.setSpoofActive(gojekTarget.id, false)
        
        val pusher = ConfigPusher(this)
        pusher.push(gojekTarget)
        
        Log.d("PROJECT-X", "Fake GPS Gojek berhasil di-STOP otomatis.")

        // KAN SINYAL BROADCAST KE UI (MAINACTIVITY)
        val intent = Intent("ACTION_GOJEK_TRIP_RECEIVED")
        sendBroadcast(intent)
    }
}

}
