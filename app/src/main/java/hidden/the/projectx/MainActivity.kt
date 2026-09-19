package hidden.the.projectx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.service.LocationService
import hidden.the.projectx.ui.PermissionFlow

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var permissionFlow: PermissionFlow

    private var tvStatus: TextView? = null
    private var btnStartService: Button? = null
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)

        // Inisialisasi komponen View
        tvStatus = findViewById(resources.getIdentifier("tvStatus", "id", packageName))
            ?: findViewById(resources.getIdentifier("tv_status", "id", packageName))

        btnStartService = findViewById(resources.getIdentifier("btnStartService", "id", packageName))
            ?: findViewById(resources.getIdentifier("btn_start_service", "id", packageName))
            ?: findViewById(resources.getIdentifier("btnStart", "id", packageName))

        // Inisialisasi PermissionFlow
        permissionFlow = PermissionFlow(
            activity = this,
            prefs = prefs,
            onGranted = {
                updateStatusUI()
            }
        ).apply {
            onSettled = {
                if (!hasPermission()) {
                    tvStatus?.text = "Status: Izin Lokasi Diperlukan"
                }
            }
        }

        // Listener untuk Tombol "MULAI LAYANAN"
        btnStartService?.setOnClickListener {
            if (!permissionFlow.hasPermission()) {
                permissionFlow.requestOrGuide()
                return@setOnClickListener
            }

            if (isServiceRunning) {
                stopMainService()
            } else {
                startMainService()
            }
        }

        // Jalankan pengecekan izin saat pertama kali dibuka
        permissionFlow.requestOrGuide()
    }

    override fun onResume() {
        super.onResume()
        permissionFlow.resumePendingBackground {
            if (permissionFlow.hasPermission()) {
                updateStatusUI()
            }
        }
    }

    private fun updateStatusUI() {
        runOnUiThread {
            if (isServiceRunning) {
                tvStatus?.text = "Status: Layanan Berjalan"
                btnStartService?.text = "HENTIKAN LAYANAN"
            } else {
                tvStatus?.text = "Status: Siap"
                btnStartService?.text = "MULAI LAYANAN"
            }
        }
    }

    private fun startMainService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isServiceRunning = true
        updateStatusUI()
        Toast.makeText(this, "Layanan berhasil dijalankan", Toast.LENGTH_SHORT).show()
    }

    private fun stopMainService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
        isServiceRunning = false
        updateStatusUI()
        Toast.makeText(this, "Layanan dihentikan", Toast.LENGTH_SHORT).show()
    }
}
