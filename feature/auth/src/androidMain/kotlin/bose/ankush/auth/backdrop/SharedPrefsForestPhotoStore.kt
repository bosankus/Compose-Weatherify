package bose.ankush.auth.backdrop

import android.content.Context
import androidx.core.content.edit

internal class SharedPrefsForestPhotoStore(
    context: Context,
) : ForestPhotoStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(day: Int): ForestPhoto? {
        if (prefs.getInt(KEY_DAY, -1) != day) return null
        return forestPhotoOrNull(FOREST_FIELDS.map { prefs.getString(it, null).orEmpty() })
    }

    override fun save(
        day: Int,
        photo: ForestPhoto,
    ) {
        prefs.edit {
            putInt(KEY_DAY, day)
            photo.fieldValues().zip(FOREST_FIELDS).forEach { (value, field) -> putString(field, value) }
        }
    }

    private companion object {
        const val PREFS = "login_forest"
        const val KEY_DAY = "day"
    }
}
