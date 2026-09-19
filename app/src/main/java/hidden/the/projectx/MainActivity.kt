import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// ... di dalam kelas MainActivity ...

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

private fun checkLocationPermission(): Boolean {
    // 1. Cek izin lokasi dasar (Fine & Coarse)
    val hasFineLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocation && !hasCoarseLocation) {
        // Minta izin lokasi utama jika belum diberikan
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        return false
    }

    // 2. Jika lokasi utama sudah aktif, lanjutkan alur aplikasi (Load Map / Services)
    onLocationPermissionGranted()
    return true
}

private fun onLocationPermissionGranted() {
    // Panggil logika lanjutan di sini (misal: initMap(), startServiceCheck(), dsb.)
    // Ini akan mengubah status "Memeriksa..." menjadi aktif/siap
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk menjalankan aplikasi", Toast.LENGTH_SHORT).show()
        }
    }
}
