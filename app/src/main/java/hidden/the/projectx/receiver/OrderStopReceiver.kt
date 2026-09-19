package hidden.the.projectx.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import hidden.the.projectx.service.FakeLocationService

class OrderStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "hidden.the.projectx.ACTION_STOP_FAKE") {
            Log.d("OrderStopReceiver", "Menerima perintah pemutusan dari Xposed Hook!")

            // Hentikan Foreground Service Fake Location
            val serviceIntent = Intent(context, FakeLocationService::class.java)
            context?.stopService(serviceIntent)
        }
    }
}
