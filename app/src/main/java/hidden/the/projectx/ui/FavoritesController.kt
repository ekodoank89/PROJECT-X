package hidden.the.projectx.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import hidden.the.projectx.R
import hidden.the.projectx.core.FavoriteItem
import hidden.the.projectx.core.FavoritesStore
import hidden.the.projectx.core.Targets

class FavoritesController(
    private val context: Context,
    private val store: FavoritesStore,
    private val centerProvider: () -> Pair<Double, Double>,
    private val onPlay: (catId: String, lat: Double, lng: Double, name: String) -> Unit,
    private val onPick: (catId: String, lat: Double, lng: Double, name: String) -> Unit
) {

    private var activeCatId: String = Targets.GRAB.id
    private var activeMode: String = "pin" // "pin" atau "manual"
    private var sheetDialog: BottomSheetDialog? = null

    fun bind(btnFavId: Int) {
        val btnFav = (context as? android.app.Activity)?.findViewById<ImageButton>(btnFavId)
        btnFav?.setOnClickListener { showDialog() }
    }

    fun showDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_favorites, null)
        sheetDialog = BottomSheetDialog(context)
        sheetDialog?.setContentView(view)

        val btnGrab = view.findViewById<TextView>(R.id.btn_cat_grab)
        val btnGojek = view.findViewById<TextView>(R.id.btn_cat_gojek)
        val btnPin = view.findViewById<TextView>(R.id.btn_mode_pin)
        val btnManual = view.findViewById<TextView>(R.id.btn_mode_manual)
        val etName = view.findViewById<EditText>(R.id.et_fav_name)
        val etLat = view.findViewById<EditText>(R.id.et_fav_lat)
        val etLng = view.findViewById<EditText>(R.id.et_fav_lng)
        val btnSave = view.findViewById<View>(R.id.btn_save_fav)
        val rvFav = view.findViewById<RecyclerView>(R.id.rv_favorites)

        rvFav.layoutManager = LinearLayoutManager(context)

        fun refreshList() {
            val list = store.get(activeCatId)
            rvFav.adapter = FavoriteAdapter(
                items = list,
                onItemClick = { item ->
                    // 1. Langsung jalankan fungsi Play (GOJEK / GRAB)
                    onPlay(item.catId, item.lat, item.lng, item.name)
                    
                    // 2. Langsung tutup menu favorit tanpa dialog konfirmasi
                    sheetDialog?.dismiss()
                },
                onDeleteClick = { item ->
                    store.remove(item.catId, item.id)
                    refreshList()
                }
            )
        }

        btnGrab.setOnClickListener {
            activeCatId = Targets.GRAB.id
            refreshList()
        }

        btnGojek.setOnClickListener {
            activeCatId = Targets.GOJEK.id
            refreshList()
        }

        btnSave?.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "Nama lokasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var (lat, lng) = centerProvider()
            if (activeMode == "manual") {
                val inputLat = etLat.text.toString().toDoubleOrNull()
                val inputLng = etLng.text.toString().toDoubleOrNull()
                if (inputLat != null && inputLng != null) {
                    lat = inputLat
                    lng = inputLng
                }
            }

            store.add(activeCatId, name, lat, lng)
            etName.setText("")
            etLat.setText("")
            etLng.setText("")
            refreshList()
            Toast.makeText(context, "Favorit disimpan", Toast.LENGTH_SHORT).show()
        }

        refreshList()
        sheetDialog?.show()
    }

    // Adapter RecyclerView Internal
    private inner class FavoriteAdapter(
        private val items: List<FavoriteItem>,
        private val onItemClick: (FavoriteItem) -> Unit,
        private val onDeleteClick: (FavoriteItem) -> Unit
    ) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_item_name)
            val tvCoords: TextView = view.findViewById(R.id.tv_item_coords)
            val btnDelete: View = view.findViewById(R.id.btn_delete_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvCoords.text = String.format("%.6f, %.6f", item.lat, item.lng)

            // AKSI TAP ITEM FAVORIT: LANGSUNG PLAY & CLOSE
            holder.itemView.setOnClickListener {
                onItemClick(item)
            }

            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
