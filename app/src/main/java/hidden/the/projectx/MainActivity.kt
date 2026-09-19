package hidden.the.projectx

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import hidden.the.projectx.core.ConfigPusher
import hidden.the.projectx.core.FavoritesStore
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.core.SpoofTarget
import hidden.the.projectx.core.Targets
import hidden.the.projectx.ui.FavoritesController
import hidden.the.projectx.ui.JitterController
import hidden.the.projectx.ui.MapController
import hidden.the.projectx.ui.NotifController
import hidden.the.projectx.ui.NotifPermissionFlow
import hidden.the.projectx.ui.PermissionFlow
import hidden.the.projectx.ui.PlayPanelController
import com.google.android.gms.maps.SupportMapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var map: MapController
    private lateinit var permissionFlow: PermissionFlow
    private lateinit var playPanel: PlayPanelController
    private lateinit var pusher: ConfigPusher
    private lateinit var notifs: NotifController
    private lateinit var favorites: FavoritesController
    private lateinit var notifPerm: NotifPermissionFlow
    private lateinit var jitter: JitterController

    // Launcher izin notifikasi — WAJIB field (terdaftar sebelum onStart).
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPerm.markAsked()
        notifPerm.notifDone?.let { it(); notifPerm.notifDone = null }
    }

    // ===== RANTAI IZIN + DOUBLE CROSS-CHECK =====
    private var lastStage = ""
    private val chainHandler = Handler(Looper.getMainLooper())
    private var batteryOnceThisSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        map = MapController(this, prefs)
        pusher = ConfigPusher(this)
        notifs = NotifController(this)
        notifPerm = NotifPermissionFlow(this, notifPermLauncher)
        favorites = FavoritesController(
            this,
            FavoritesStore(this),
            centerProvider = { map.currentCenter() },
            onPlay = { catId, lat, lng, name ->
                playFromFavorite(catId, lat, lng, name)
            },
            onPick = { catId, lat, lng, name ->
                playFromFavorite(catId, lat, lng, name)
            }
        )

        permissionFlow = PermissionFlow(this, prefs) {
            map.ensureBlueDot()
            map.focusFresh()
            refreshUIState()
        }

        permissionFlow.onSettled = { nextChainStep() }

        // PlayPanel mem-push sendiri saat toggle
        playPanel = PlayPanelController(
            this, prefs,
            pusher = pusher,
            centerProvider = { map.currentCenter() }
        ) { target, active ->
            if (active) {
                if (!notifs.canNotify()) {
                    notifPerm.ensureBeforePlay { }
                }
            }
            refreshNotif()
            announce(target, active)
        }

        favorites.bind(R.id.btn_fav)

        // ==== JITTER ====
        jitter = JitterController(this, prefs, pusher)
        jitter.bind(R.id.btn_jitter)

        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener { map.zoomMax() }
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener { map.zoomOut() }
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener {
            permissionFlow.requestOrGuide()
        }
        findViewById<ImageButton>(R.id.btn_theme).setOnClickListener {
            prefs.isDark = !prefs.isDark
            map.applyStyle(prefs.isDark)
            updateThemeIcon()
        }
        updateThemeIcon()

        // Bind Play Panel UI & Attach Google Map Fragment
        playPanel.bind()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.let { map.attach(it) }

        // Inisialisasi Pengecekan Izin Pertama Kali
        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
            map.focusFresh()
            refreshUIState()
            nextChainStep()
        } else {
            permissionFlow.requestOrGuide()
        }
    }

    /** Memperbarui status tampilan PlayPanel & Notifikasi indikator. */
    private fun refreshUIState() {
        runOnUiThread {
            playPanel.bind()
            refreshNotif()
        }
    }

    /** Satu pintu update notifikasi indikator (kumpulkan target aktif → update). */
    private fun refreshNotif() {
        val activeList = Targets.all.mapNotNull { t ->
            if (prefs.isSpoofActive(t.id)) {
                prefs.spoofPoint(t.id)?.let { t to it }
            } else null
        }
        notifs.update(activeList)
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
            refreshUIState()
        }

        // Kembali dari Settings → selesaikan tahap tertunda → rantai evaluasi ulang
        permissionFlow.resumePendingBackground { nextChainStep() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::map.isInitialized) map.stop()
    }

    /**
     * Mesin status rantai izin:
     * 1) Lokasi dasar   — ulang hingga granted
     * 2) Selalu izinkan — ulang hingga granted
     * 3) Notifikasi     — ulang hingga granted
     * 4) Baterai        — dialog sistem SEKALI per sesi
     */
    private fun nextChainStep() {
        when {
            !permissionFlow.hasPermission() ->
                beginStage("Lokasi") { permissionFlow.requestOrGuide() }

            !permissionFlow.hasBackgroundLocation() ->
                beginStage("Selalu izinkan") {
                    permissionFlow.requestBackgroundLocation { 
                        refreshUIState()
                        nextChainStep() 
                    }
                }

            !notifPerm.isGranted() ->
                beginStage("Notifikasi") {
                    notifPerm.requestInChain { 
                        refreshUIState()
                        nextChainStep() 
                    }
                }

            !permissionFlow.isBatteryUnrestricted() -> {
                if (!batteryOnceThisSession) {
                    batteryOnceThisSession = true
                    permissionFlow.requestBatteryExemption { 
                        refreshUIState()
                    }
                }
            }
            else -> {
                // Semua izin selesai
                refreshUIState()
            }
        }
    }

    private fun beginStage(name: String, request: () -> Unit) {
        if (name == lastStage) {
            Toast.makeText(
                this,
                "Izin \"$name\" belum aktif — mengulangi permintaan",
                Toast.LENGTH_SHORT
            ).show()
            chainHandler.postDelayed({ request() }, 700)
        } else {
            lastStage = name
            request()
        }
    }

    private fun playFromFavorite(catId: String, lat: Double, lng: Double, name: String) {
        val target = Targets.byId(catId)
        prefs.setSpoofPoint(catId, lat, lng)
        prefs.setSpoofActive(catId, true)
        pusher.push(target)
        playPanel.refresh(catId)
        refreshNotif()

        val launch = packageManager.getLaunchIntentForPackage(
            target.packageNames.firstOrNull() ?: return
        )
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }

        Toast.makeText(this,
            "${target.label} AKTIF di \"$name\" — membuka aplikasi…",
            Toast.LENGTH_SHORT).show()
    }

    private fun announce(target: SpoofTarget, active: Boolean) {
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()} — membuka aplikasi…"
                  else "${target.label} dihentikan — notif hilang"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        findViewById<ImageButton>(R.id.btn_theme)
            .setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
