package hidden.the.projectx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.ui.FavoritesController
import hidden.the.projectx.ui.FavoritesStore
import hidden.the.projectx.ui.MapController

class MainActivity : AppCompatActivity() {

    // Action nama Broadcast saat service berhenti (Gojek / Grab / General)
    companion object {
        const val ACTION_SERVICE_STOPPED = "hidden.the.projectx.ACTION_SERVICE_STOPPED"
        const val ACTION_AUTO_STOP = "hidden.the.projectx.ACTION_AUTO_STOP"
    }

    private lateinit var prefs: Prefs
    private lateinit var map: MapController
    private lateinit var favorites: FavoritesController

    // 1. Receiver untuk menangkap event Auto Stop dari Service
    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_SERVICE_STOPPED || action == ACTION_AUTO_STOP) {
                // Pastikan pembaruan UI dieksekusi di Main Thread
                runOnUiThread {
                    updatePlayStopUI()
                    Toast.makeText(this@MainActivity, "Spoofing / Auto Fake telah berhenti", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        
        // Inisialisasi FavoritesController dengan animasi flyTo & close otomatis
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

        // Register Broadcast Receiver
        registerServiceReceiver()

        // Sync UI pertama kali
        updatePlayStopUI()
    }

    override fun onResume() {
        super.onResume()
        // 2. Selalu update UI tombol saat aplikasi dibuka kembali ke foreground
        updatePlayStopUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver agar tidak memory leak
        try {
            unregisterReceiver(serviceStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Memperbarui visibilitas tombol Play & Stop berdasarkan status service yang berjalan
     */
    fun updatePlayStopUI() {
        // Ambil status service dari Prefs atau Status Tracker Anda
        val isGojekRunning = prefs.isGojekRunning
        val isGrabRunning  = prefs.isGrabRunning

        // --- UPDATE UI GOJEK ---
        val btnPlayGojek = findViewById<View>(R.id.btn_play_gojek) // Sesuaikan ID layout Anda
        val btnStopGojek = findViewById<View>(R.id.btn_stop_gojek) // Sesuaikan ID layout Anda

        if (btnPlayGojek != null && btnStopGojek != null) {
            if (isGojekRunning) {
                btnPlayGojek.visibility = View.GONE
                btnStopGojek.visibility = View.VISIBLE
            } else {
                btnPlayGojek.visibility = View.VISIBLE
                btnStopGojek.visibility = View.GONE
            }
        }

        // --- UPDATE UI GRAB ---
        val btnPlayGrab = findViewById<View>(R.id.btn_play_grab) // Sesuaikan ID layout Anda
        val btnStopGrab = findViewById<View>(R.id.btn_stop_grab) // Sesuaikan ID layout Anda

        if (btnPlayGrab != null && btnStopGrab != null) {
            if (isGrabRunning) {
                btnPlayGrab.visibility = View.GONE
                btnStopGrab.visibility = View.VISIBLE
            } else {
                btnPlayGrab.visibility = View.VISIBLE
                btnStopGrab.visibility = View.GONE
            }
        }
    }

    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_SERVICE_STOPPED)
            addAction(ACTION_AUTO_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStateReceiver, filter)
        }
    }

    private fun playFromFavorite(catId: String, lat: Double, lng: Double, name: String) {
        // Logika spoofing lokasi favorit Anda
        updatePlayStopUI()
    }
}
