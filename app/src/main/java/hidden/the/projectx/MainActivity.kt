package hidden.the.projectx

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import hidden.the.projectx.core.Prefs
import hidden.the.projectx.ui.PermissionFlow

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var permissionFlow: PermissionFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)

        // Inisialisasi PermissionFlow
        permissionFlow = PermissionFlow(
            activity = this,
            prefs = prefs,
            onGranted = {
                // Callback saat izin lokasi aktif
                updateStatusUI()
            }
        ).apply {
            onSettled = {
                if (!hasPermission()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Izin lokasi diperlukan untuk menjalankan aplikasi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Jalankan pengecekan izin saat pertama kali dibuka
        permissionFlow.requestOrGuide()
    }

    override fun onResume() {
        super.onResume()
        // Tangkap status setelah user kembali dari Pengaturan (Settings)
        permissionFlow.resumePendingBackground {
            if (permissionFlow.hasPermission()) {
                updateStatusUI()
            }
        }
    }

    /**
     * Memperbarui status UI saat izin lokasi telah diberikan
     */
    private fun updateStatusUI() {
        runOnUiThread {
            // Mencari TextView status berdasarkan ID umum atau mengubah status secara internal
            val tvStatus = findViewById<TextView>(R.id.tvStatus)
                ?: findViewById<TextView>(resources.getIdentifier("tv_status", "id", packageName))

            tvStatus?.text = "Status: Siap"
        }
    }
}
