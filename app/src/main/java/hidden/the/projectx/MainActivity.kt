package hidden.the.projectx

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.ui.PermissionFlow

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var permissionFlow: PermissionFlow

    private lateinit var tvStatus: TextView
    private lateinit var btnStartService: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)

        // Inisialisasi Komponen UI dari Layout
        tvStatus = findViewById(R.id.tvStatus) // Sesuaikan ID jika berbeda di layout Anda
        btnStartService = findViewById(R.id.btnStartService) // Sesuaikan ID jika berbeda

        // Inisialisasi PermissionFlow
        permissionFlow = PermissionFlow(
            activity = this,
            prefs = prefs,
            onGranted = {
                // Dipanggil saat "Izin lokasi diberikan"
                updateUIOnPermissionGranted()
            }
        ).apply {
            onSettled = {
                if (!hasPermission()) {
                    tvStatus.text = "Status: Izin Lokasi Diperlukan"
                }
            }
        }

        // Setup Listener Tombol Mulai Layanan
        btnStartService.setOnClickListener {
            if (permissionFlow.hasPermission()) {
                startMainService()
            } else {
                permissionFlow.requestOrGuide()
            }
        }

        // Jalankan pengecekan izin saat pertama kali dibuka
        permissionFlow.requestOrGuide()
    }

    override fun onResume() {
        super.onResume()
        // Tangkap kembali jika user mengubah izin via Settings
        permissionFlow.resumePendingBackground {
            if (permissionFlow.hasPermission()) {
                updateUIOnPermissionGranted()
            }
        }
    }

    /**
     * Memperbarui UI dari "Memeriksa..." menjadi Siap setelah izin diberikan
     */
    private fun updateUIOnPermissionGranted() {
        runOnUiThread {
            tvStatus.text = "Status: Siap / Layanan Nonaktif"
            // Atau jika layanan sudah berjalan: tvStatus.text = "Status: Layanan Aktif"
        }
    }

    private fun startMainService() {
        // Logika untuk menjalankan layanan/spoofing Anda
        tvStatus.text = "Status: Layanan Berjalan"
        Toast.makeText(this, "Layanan berhasil dimulaikan", Toast.LENGTH_SHORT).show()
    }
}
