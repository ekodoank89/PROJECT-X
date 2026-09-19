package hidden.the.projectx.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hidden.the.projectx.R
import hidden.the.projectx.core.Targets

/**
 * Controller untuk pengaturan Jitter (Randomize Koordinat).
 * Mengingat tab posisi (GRAB | GOJEK) terakhir yang digunakan.
 */
class JitterController(
    private val activity: Activity,
    private val onJitterChanged: (catId: String, enabled: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null

    companion object {
        // Menyimpan tab kategori terakhir Jitter
        private var lastSelectedCategory: String = Targets.GRAB.id
    }

    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    private fun lebarkan(d: AlertDialog) {
        d.setOnShowListener {
            d.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_jitter, null)
        val catGrab  = v.findViewById<TextView>(R.id.cat_grab)
        val catGojek = v.findViewById<TextView>(R.id.cat_gojek)

        var cat = lastSelectedCategory

        fun setCat(c: String) {
            cat = c
            lastSelectedCategory = c // Simpan status tab terakhir
            
            val sel = R.drawable.bg_mode_on
            val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt()
            val off = 0x99FFFFFF.toInt()

            catGrab.setBackgroundResource(if (c == Targets.GRAB.id) sel else unsel)
            catGrab.setTextColor(if (c == Targets.GRAB.id) on else off)
            catGojek.setBackgroundResource(if (c == Targets.GOJEK.id) sel else unsel)
            catGojek.setTextColor(if (c == Targets.GOJEK.id) on else off)
        }

        catGrab.setOnClickListener { setCat(Targets.GRAB.id) }
        catGojek.setOnClickListener { setCat(Targets.GOJEK.id) }

        dialog = AlertDialog.Builder(activity, R.style.Theme_PROJECTX_Dialog)
            .setView(v)
            .create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        lebarkan(dialog!!)

        // Terapkan kategori yang diingat sebelum dialog muncul
        setCat(cat)
        dialog?.show()
    }
}
