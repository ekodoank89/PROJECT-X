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

    // Receiver untuk mendengarkan event Auto Fake Stop dari Service
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
        pusher = ConfigPusher(this, prefs)

        // Inisialisasi MapController
        map = MapController(this, prefs)

        // Inisialisasi FavoritesController
        // Ketika list favorite di-tap: dialog otomatis close -> peta flyTo ke titik lokasi -> jalankan spoofing
        favorites = FavoritesController(
            this,
            prefs,
            pusher,
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

        // Inisialisasi JitterController
        jitter = JitterController(this, prefs, pusher)

        // Register receiver untuk sinkronisasi Auto Stop
        registerServiceReceiver()

        // Sync UI status tombol saat awal terbuka
        updatePlayStopUI()
    }

    override fun onResume() {
        super.onResume()
        // Memastikan status UI tombol selalu fresh saat aplikasi kembali aktif
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

    /**
     * Memperbarui status UI tombol saat Auto Fake Stop dipicu atau service berhenti
     */
    fun updatePlayStopUI() {
        runOnUiThread {
            // Menggunakan ConfigPusher untuk menyinkronkan status service dan UI tombol secara internal
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
        // Logika spoofing lokasi favorit
        updatePlayStopUI()
    }
}
