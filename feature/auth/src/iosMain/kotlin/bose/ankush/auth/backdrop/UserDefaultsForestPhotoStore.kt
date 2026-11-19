package bose.ankush.auth.backdrop

import platform.Foundation.NSUserDefaults

/** Same keys as before the picker moved to shared code, so today's stored pick still reads back. */
internal class UserDefaultsForestPhotoStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : ForestPhotoStore {
    override fun load(day: Int): ForestPhoto? {
        if (defaults.integerForKey(KEY_DAY).toInt() != day) return null
        return forestPhotoOrNull(FOREST_FIELDS.map { defaults.stringForKey(KEY_PREFIX + it).orEmpty() })
    }

    override fun save(
        day: Int,
        photo: ForestPhoto,
    ) {
        defaults.setInteger(day.toLong(), forKey = KEY_DAY)
        photo.fieldValues().zip(FOREST_FIELDS).forEach { (value, field) ->
            defaults.setObject(value, forKey = KEY_PREFIX + field)
        }
    }

    private companion object {
        const val KEY_DAY = "login_forest.day"
        const val KEY_PREFIX = "login_forest."
    }
}
