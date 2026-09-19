package hidden.the.projectx.core

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class ConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        // Harus mengembalikan true dan tidak boleh melempar Exception saat startup
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.item/vnd.hidden.the.projectx.config"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}
