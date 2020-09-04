package org.greenstand.android.TreeTracker.preferences

import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.managers.UserManager

class PreferencesMigrator(
    private val sharedPreferences: SharedPreferences,
    private val preferences: Preferences
) {

    fun migrateIfNeeded() {
        if (!preferences.getBoolean(IS_MIGRATION_DONE_KEY, false)) {
            migrate()
        }
    }

    private fun migrate() {
        preferences.edit()
            .putString(UserManager.FIRST_NAME_KEY, sharedPreferences.getString("FIRST_NAME_KEY", "") ?: "")
            .putString(UserManager.LAST_NAME_KEY, sharedPreferences.getString("LAST_NAME_KEY", "") ?: "")
            .putString(UserManager.ORG_NAME_KEY, sharedPreferences.getString("ORG_NAME_KEY", "") ?: "")
            .putLong(UserManager.PLANTER_CHECK_IN_ID_KEY, sharedPreferences.getLong("PLANTER_CHECK_IN_ID", -1))
            .putLong(UserManager.PLANTER_INFO_ID_KEY, sharedPreferences.getLong("PLANTER_INFO_ID", -1))
            .putString(UserManager.PROFILE_PHOTO_PATH_KEY, sharedPreferences.getString("PLANTER_PHOTO", ""))
            .putLong(UserManager.LAST_CHECK_IN_TIME_IN_KEY, sharedPreferences.getLong("TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS", -1))
            .putBoolean(IS_MIGRATION_DONE_KEY, true)
            .commit()
    }

    companion object {
        private val IS_MIGRATION_DONE_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("pref-migration")
    }
}
