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

    // Launcher izin notifikasi
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

        // Dapatkan Res ID secara dinamis untuk menghindari Unresolved Reference
        val favId = getResId("btn_fav")
        if (favId != 0) favorites.bind(favId)

        jitter = JitterController(this, prefs, pusher)
        val jitterId = getResId("btn_jitter")
        if (jitterId != 0) jitter.bind(jitterId)

        findViewByName<Button>("btn_zoom_in")?.setOnClickListener { map.zoomMax() }
        findViewByName<Button>("btn_zoom_out")?.setOnClickListener { map.zoomOut() }
        findViewByName<ImageButton>("btn_my_location")?.setOnClickListener {
            permissionFlow.requestOrGuide()
        }
        findViewByName<ImageButton>("btn_theme")?.setOnClickListener {
            prefs.isDark = !prefs.isDark
            map.applyStyle(prefs.isDark)
            updateThemeIcon()
        }
        updateThemeIcon()

        // Bind Play Panel UI
        playPanel.bind()

        // Safely Bind Google Map Fragment
        val mapResId = getResId("map")
        val mapFragment = if (mapResId != 0) {
            supportFragmentManager.findFragmentById(mapResId) as? SupportMapFragment
        } else null

        mapFragment?.let { map.attach(it) }

        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
            map.focusFresh()
            refreshUIState()
            nextChainStep()
        } else {
            permissionFlow.requestOrGuide()
        }
    }

    private fun refreshUIState() {
        runOnUiThread {
            playPanel.bind()
            refreshNotif()
        }
    }

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

        permissionFlow.resumePendingBackground { nextChainStep() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::map.isInitialized) map.stop()
    }

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
        val iconRes = if (prefs.isDark) getResId("ic_sun", "drawable") else getResId("ic_moon", "drawable")
        findViewByName<ImageButton>("btn_theme")?.let { btn ->
            if (iconRes != 0) btn.setImageResource(iconRes)
        }
    }

    // Helper functions untuk menghindari Unresolved Reference pada kompilasi Release
    private fun getResId(name: String, type: String = "id"): Int {
        return resources.getIdentifier(name, type, packageName)
    }

    private fun <T : android.view.View> findViewByName(name: String): T? {
        val id = getResId(name, "id")
        return if (id != 0) findViewById(id) else null
    }
}
