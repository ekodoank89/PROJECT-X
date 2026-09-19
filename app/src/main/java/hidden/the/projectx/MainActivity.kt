package hidden.the.projectx

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import hidden.the.projectx.core.NotificationHelper
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.core.Targets
import hidden.the.projectx.ui.FavoritesController
import hidden.the.projectx.ui.JitterController
import hidden.the.projectx.ui.MapController

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_AUTO_STOP = "hidden.the.projectx.ACTION_AUTO_STOP"
        const val ACTION_SERVICE_STOPPED = "hidden.the.projectx.ACTION_SERVICE_STOPPED"
    }

    private lateinit var prefs: Prefs
    private lateinit var pusher: ConfigPusher
    private lateinit var map: MapController
    private lateinit var favorites: FavoritesController
    private lateinit var jitter: JitterController
    private lateinit var notificationHelper: NotificationHelper

    private var btnGrab: ImageButton? = null
    private var btnGojek: ImageButton? = null

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

    // Permission Launcher khusus Notifikasi (Android 13+)
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Izin notifikasi ditolak. Notifikasi status bar tidak dapat ditampilkan.", Toast.LENGTH_SHORT).show()
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
        notificationHelper = NotificationHelper(this)

        // Binding View Tombol Utama
        btnGrab = findViewById(R.id.btn_grab)
        btnGojek = findViewById(R.id.btn_gojek)

        // 1. Inisialisasi MapController & Attach Fragment
        map = MapController(this, prefs)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.let {
            map.attach(it)
        }

        // 2. Setup Click Listener Tombol Navigasi Peta
        setupMapControlButtons()

        // 3. Minta Izin Lokasi & Notifikasi
        checkAndEnablePermissions()

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
        btnGrab?.setOnClickListener {
            toggleTarget(Targets.GRAB.id)
        }

        // Tombol Play / Stop GOJEK (@id/btn_gojek)
        btnGojek?.setOnClickListener {
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

        val currentlyActive = prefs.isSpoofActive(targetId)
        val nextState = !currentlyActive

        // Simpan koordinat ke SharedPreferences via Prefs & atur status aktif
        prefs.setSpoofPoint(targetId, center.latitude, center.longitude)
        prefs.setSpoofActive(targetId, nextState)

        // Broadcast perubahan ke target & perbarui UI
        updatePlayStopUI()

        val statusText = if (nextState) "Aktif" else "Mati"
        Toast.makeText(this, "Lokasi ${targetId.uppercase()} ($statusText)", Toast.LENGTH_SHORT).show()

        // Buka aplikasi target jika status berubah ke aktif (PLAY)
        if (nextState) {
            launchTargetApp(targetId)
        }
    }

    private fun checkAndEnablePermissions() {
        // 1. Izin Lokasi
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

        // 2. Izin Notifikasi (Android 13 / Tiramisu ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
            // Push konfigurasi terbaru ke broadcast receiver
            pusher.pushAll()

            val isGrabActive = prefs.isSpoofActive(Targets.GRAB.id)
            val isGojekActive = prefs.isSpoofActive(Targets.GOJEK.id)

            // Update Background & Icon Tombol GRAB
            btnGrab?.setBackgroundResource(
                if (isGrabActive) R.drawable.bg_play_red_touch else R.drawable.bg_play_green_touch
            )
            btnGrab?.setImageResource(
                if (isGrabActive) R.drawable.ic_stop else R.drawable.ic_play
            )

            // Update Background & Icon Tombol GOJEK
            btnGojek?.setBackgroundResource(
                if (isGojekActive) R.drawable.bg_play_red_touch else R.drawable.bg_play_green_touch
            )
            btnGojek?.setImageResource(
                if (isGojekActive) R.drawable.ic_stop else R.drawable.ic_play
            )

            // Update Status Notifikasi di Status Bar
            updateStatusBarNotification(isGrabActive, isGojekActive)
        }
    }

    private fun updateStatusBarNotification(isGrabActive: Boolean, isGojekActive: Boolean) {
        val activeTargets = mutableListOf<String>()
        if (isGrabActive) activeTargets.add("GRAB")
        if (isGojekActive) activeTargets.add("GOJEK")

        if (activeTargets.isNotEmpty()) {
            val targetsText = activeTargets.joinToString(" & ")
            notificationHelper.showOrUpdate(
                title = "Fake GPS Active ($targetsText)",
                message = "Lokasi spoofing sedang aktif di background."
            )
        } else {
            notificationHelper.cancel()
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
        val combinedText = "${catId.lowercase()} ${name.lowercase()}"

        val targetIds = when {
            combinedText.contains("gojek") -> listOf(Targets.GOJEK.id)
            combinedText.contains("grab") -> listOf(Targets.GRAB.id)
            else -> listOf(Targets.GOJEK.id, Targets.GRAB.id)
        }

        for (targetId in targetIds) {
            prefs.setSpoofPoint(targetId, lat, lng)
            prefs.setSpoofActive(targetId, true)
        }

        pusher.pushAll()
        updatePlayStopUI()

        Toast.makeText(this, "Meluncur ke $name", Toast.LENGTH_SHORT).show()

        for (targetId in targetIds) {
            launchTargetApp(targetId)
        }
    }

    private fun launchTargetApp(targetId: String) {
        val targetLower = targetId.lowercase()

        val packagesToTry = when {
            targetLower.contains("gojek") -> listOf("com.gojek.partner", "com.gojek.app")
            targetLower.contains("grab") -> listOf("com.grabtaxi.driver2", "com.grabtaxi.passenger")
            else -> emptyList()
        }

        for (pkg in packagesToTry) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    startActivity(launchIntent)
                    Log.d(TAG, "Berhasil membuka aplikasi: $pkg")
                    return
                } else {
                    val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(fallbackIntent)
                    Log.d(TAG, "Berhasil membuka via fallback intent: $pkg")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membuka $pkg: ${e.message}")
            }
        }

        Toast.makeText(this, "Aplikasi target ($targetId) tidak terpasang di HP ini", Toast.LENGTH_SHORT).show()
    }
}
