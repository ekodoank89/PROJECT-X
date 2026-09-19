package hidden.the.projectx.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import hidden.the.projectx.service.LocationService

class OrderStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "hidden.the.projectx.ACTION_STOP_FAKE") {
            Log.d("OrderStopReceiver", "Menerima sinyal stop dari Xposed Hook!")

            context?.let {
                val serviceIntent = Intent(it, LocationService::class.java)
                it.stopService(serviceIntent)
            }
        }
    }
}
