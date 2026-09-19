package hidden.the.projectx

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import hidden.the.projectx.service.LocationService

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnToggleService: Button
    private var isServiceRunning = false

    private val PERMISSION_REQUEST_CODE = 101

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isRunning = intent?.getBooleanExtra("is_running", false) ?: false
            updateUiState(isRunning)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnToggleService = findViewById(R.id.btnToggleService)

        btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopLocationService()
            } else {
                checkAndRequestPermissions()
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter("hidden.the.projectx.LOCATION_STATUS_UPDATE")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        try {
            val serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUiState(true)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menjalankan service: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationService() {
        try {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            updateUiState(false)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menghentikan service: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUiState(running: Boolean) {
        isServiceRunning = running
        if (running) {
            tvStatus.text = "Status: BERJALAN"
            tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Hijau
            btnToggleService.text = "HENTIKAN LAYANAN"
        } else {
            tvStatus.text = "Status: MATI"
            tvStatus.setTextColor(Color.parseColor("#F44336")) // Merah
            btnToggleService.text = "MULAI LAYANAN"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationService()
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk menjalankan aplikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
