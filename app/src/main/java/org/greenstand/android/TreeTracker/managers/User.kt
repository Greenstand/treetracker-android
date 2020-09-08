package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.koin.core.context.GlobalContext

class User(
    private val preferences: Preferences
) {

    val isLoggedIn: Boolean
        get() = planterCheckinId != -1L

    var firstName: String?
        get() = preferences.getString(FIRST_NAME_KEY)
        set(value) = preferences.edit().putString(FIRST_NAME_KEY, value).apply()

    var lastName: String?
        get() = preferences.getString(LAST_NAME_KEY)
        set(value) = preferences.edit().putString(LAST_NAME_KEY, value).apply()

    var profilePhotoPath: String?
        get() = preferences.getString(PROFILE_PHOTO_PATH_KEY)
        set(value) = preferences.edit().putString(PROFILE_PHOTO_PATH_KEY, value).apply()

    var organization: String?
        get() = preferences.getString(ORG_NAME_KEY)
        set(value) = preferences.edit().putString(ORG_NAME_KEY, value).apply()

    var planterCheckinId: Long?
        get() = preferences.getLong(PLANTER_CHECK_IN_ID_KEY)
        set(value) = preferences.edit().putLong(PLANTER_CHECK_IN_ID_KEY, value ?: -1).apply()

    var planterInfoId: Long?
        get() = preferences.getLong(PLANTER_INFO_ID_KEY)
        set(value) = preferences.edit().putLong(PLANTER_INFO_ID_KEY, value ?: -1).apply()
            .also { preferences.setPlanterInfoId(value) }

    var lastCheckInTimeInSeconds: Long?
        get() = preferences.getLong(LAST_CHECK_IN_TIME_IN_KEY)
        set(value) = preferences.edit().putLong(LAST_CHECK_IN_TIME_IN_KEY, value ?: -1).apply()

    fun expireCheckInStatus() = preferences.clearPrefKeyUsage(BASE_KEY)

    companion object {
        private val BASE_KEY = PrefKeys.SESSION + PrefKey("info")
        val PLANTER_CHECK_IN_ID_KEY = BASE_KEY + PrefKey("planter-check-in-id")
        val PLANTER_INFO_ID_KEY = BASE_KEY + PrefKey("planter-info-id")
        val ORG_NAME_KEY = BASE_KEY + PrefKey("organization")
        val FIRST_NAME_KEY = BASE_KEY + PrefKey("first-name")
        val LAST_NAME_KEY = BASE_KEY + PrefKey("last-name")
        val PROFILE_PHOTO_PATH_KEY = BASE_KEY + PrefKey("profile-photo-path")
        val LAST_CHECK_IN_TIME_IN_KEY = BASE_KEY + PrefKey("last-check-in-time-in-seconds")
    }
}
