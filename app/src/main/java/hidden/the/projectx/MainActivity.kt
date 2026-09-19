package hidden.the.projectx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import hidden.the.projectx.core.ConfigPusher
import hidden.the.projectx.core.FavoritesStore
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.ui.FavoritesController
import hidden.the.projectx.ui.JitterController
import hidden.the.projectx.ui.MapController

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_AUTO_STOP = "hidden.the.projectx.ACTION_AUTO_STOP"
        const val ACTION_SERVICE_STOPPED = "hidden.the.projectx.ACTION_SERVICE_STOPPED"
    }

    private lateinit var prefs: Prefs
    private lateinit var pusher: ConfigPusher
    private lateinit var map: MapController
    private lateinit var favorites: FavoritesController
    private lateinit var jitter: JitterController

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_AUTO_STOP || action == ACTION_SERVICE_STOPPED) {
                updatePlayStopUI()
                Toast.makeText(this@MainActivity, "Auto Fake Stop dipicu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        // Fix 1: ConfigPusher hanya menerima 1 parameter (Context)
        pusher = ConfigPusher(this)

        map = MapController(this, prefs)

        // Fix 2: FavoritesController menggunakan FavoritesStore sebagai parameter ke-2
        favorites = FavoritesController(
            this,
            FavoritesStore(this),
            centerProvider = { map.currentCenter() },
            onPlay = { catId, lat, lng, name ->
                map.flyTo(LatLng(lat, lng))
                playFromFavorite(catId, lat, lng, name)
            },
            onPick = { catId, lat, lng, name ->
                map.flyTo(LatLng(lat, lng))
                playFromFavorite(catId, lat, lng, name)
            }
        )

        jitter = JitterController(this, prefs, pusher)

        registerServiceReceiver()
        updatePlayStopUI()
    }

    override fun onResume() {
        super.onResume()
        updatePlayStopUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(serviceStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updatePlayStopUI() {
        runOnUiThread {
            pusher.pushAll()
        }
    }

    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_AUTO_STOP)
            addAction(ACTION_SERVICE_STOPPED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStateReceiver, filter)
        }
    }

    private fun playFromFavorite(catId: String, lat: Double, lng: Double, name: String) {
        updatePlayStopUI()
    }
}
