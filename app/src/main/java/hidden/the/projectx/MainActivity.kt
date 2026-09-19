package hidden.the.projectx

import android.os.Bundle
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

        // Inisialisasi rantai izin
        permissionFlow = PermissionFlow(
            activity = this,
            prefs = prefs,
            onGranted = {
                // Dipanggil saat izin lokasi aktif
                setupMainUI()
            }
        ).apply {
            // Dipanggil saat pemeriksaan izin selesai (apapun hasilnya)
            onSettled = {
                if (!hasPermission()) {
                    Toast.makeText(this@MainActivity, "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Minta atau periksa izin saat aplikasi dibuka
        permissionFlow.requestOrGuide()
    }

    override fun onResume() {
        super.onResume()
        // Menyelesaikan pemeriksaan background location jika baru kembali dari Settings
        permissionFlow.resumePendingBackground {
            // Alur dilanjutkan setelah kembali dari menu Pengaturan
        }
    }

    private fun setupMainUI() {
        // Pindah/ganti status "Status: Memeriksa..." ke Peta atau UI Utama di sini
        // Contoh: initMapController(), loadPanelUI(), dll.
    }
}
