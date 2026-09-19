package hidden.the.projectx

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import hidden.the.projectx.core.ConfigPusher
import hidden.the.projectx.core.FavoritesStore
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.core.Targets
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

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (fineGranted || coarseGranted) {
            map.ensureBlueDot()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk menampilkan titik biru", Toast.LENGTH_SHORT).show()
        }
    }

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
        pusher = ConfigPusher(this)

        // 1. Inisialisasi MapController & Attach Fragment
        map = MapController(this, prefs)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.let {
            map.attach(it)
        }

        // 2. Setup Click Listener Tombol Navigasi Peta
        setupMapControlButtons()

        // 3. Minta Izin & Aktifkan Titik Biru
        checkAndEnableLocation()

        // 4. Inisialisasi FavoritesController
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

        // 5. Inisialisasi JitterController
        jitter = JitterController(this, prefs, pusher)

        // 6. Setup Click Listener Bottom Panel (GRAB, GOJEK, FAVORIT, JITTER)
        setupBottomPanelButtons()

        registerServiceReceiver()
        updatePlayStopUI()
    }

    private fun setupMapControlButtons() {
        // Tombol Fokus Lokasi Saya (@id/btn_my_location)
        findViewById<ImageButton>(R.id.btn_my_location)?.setOnClickListener {
            map.focusFresh()
        }

        // Tombol Ganti Tema Peta (@id/btn_theme)
        findViewById<ImageButton>(R.id.btn_theme)?.setOnClickListener {
            val newDarkState = !prefs.isDark
            prefs.isDark = newDarkState
            map.applyStyle(newDarkState)
        }

        // Tombol Zoom In (@id/btn_zoom_in)
        findViewById<View>(R.id.btn_zoom_in)?.setOnClickListener {
            map.zoomMax()
        }

        // Tombol Zoom Out (@id/btn_zoom_out)
        findViewById<View>(R.id.btn_zoom_out)?.setOnClickListener {
            map.zoomOut()
        }
    }

    private fun setupBottomPanelButtons() {
        // Tombol Play / Stop GRAB (@id/btn_grab)
        findViewById<ImageButton>(R.id.btn_grab)?.setOnClickListener {
            toggleTarget(Targets.GRAB.id)
        }

        // Tombol Play / Stop GOJEK (@id/btn_gojek)
        findViewById<ImageButton>(R.id.btn_gojek)?.setOnClickListener {
            toggleTarget(Targets.GOJEK.id)
        }

        // Tombol Favorit (@id/btn_fav)
        findViewById<ImageButton>(R.id.btn_fav)?.setOnClickListener {
            favorites.show()
        }

        // Tombol Jitter (@id/btn_jitter)
        findViewById<ImageButton>(R.id.btn_jitter)?.setOnClickListener {
            jitter.show()
        }
    }

    private fun toggleTarget(targetId: String) {
        val center = map.currentCenter()
        if (center == null) {
            Toast.makeText(this, "Peta belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        // Simpan koordinat dari titik tengah peta & push konfigurasi
        prefs.setLat(targetId, center.latitude)
        prefs.setLng(targetId, center.longitude)

        updatePlayStopUI()
        Toast.makeText(this, "Lokasi ${targetId.uppercase()} diperbarui", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndEnableLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            map.ensureBlueDot()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updatePlayStopUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.stop()
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
        prefs.setLat(catId, lat)
        prefs.setLng(catId, lng)
        updatePlayStopUI()
        Toast.makeText(this, "Meluncur ke $name", Toast.LENGTH_SHORT).show()
    }
}
