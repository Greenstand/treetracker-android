package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class UserManager(private val context: Context,
                  private val sharedPreferences: SharedPreferences) {

    var authToken: String? = null

    val isLoggedIn: Boolean
        get() = planterCheckinId != -1L

    var firstName: String?
        get() = sharedPreferences.getString(FIRST_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(FIRST_NAME_KEY, value).apply()

    var lastName: String?
        get() = sharedPreferences.getString(LAST_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(LAST_NAME_KEY, value).apply()

    var organization: String?
        get() = sharedPreferences.getString(ORG_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(ORG_NAME_KEY, value).apply()

    var planterCheckinId: Long?
        get() = sharedPreferences.getLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)
        set(value) = sharedPreferences.edit().putLong(ValueHelper.PLANTER_CHECK_IN_ID, value ?: -1).apply()

    fun clearUser() {
        sharedPreferences.edit().apply {
            putLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, 0)
            putString(ValueHelper.PLANTER_PHOTO, null)
            putLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)
            putLong(ValueHelper.PLANTER_INFO_ID, -1)
        }.apply()
    }

    companion object {
        private const val FIRST_NAME_KEY = "FIRST_NAME_KEY"
        private const val LAST_NAME_KEY = "LAST_NAME_KEY"
        private const val ORG_NAME_KEY = "ORG_NAME_KEY"
    }
}