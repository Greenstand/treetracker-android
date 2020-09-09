package org.greenstand.android.TreeTracker.preferences

import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.managers.User

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
        // Put old system values into new system
        preferences.edit()
            .putString(User.FIRST_NAME_KEY, sharedPreferences.getString("FIRST_NAME_KEY", "") ?: "")
            .putString(User.FIRST_NAME_KEY, sharedPreferences.getString("FIRST_NAME_KEY", "") ?: "")
            .putString(User.LAST_NAME_KEY, sharedPreferences.getString("LAST_NAME_KEY", "") ?: "")
            .putString(User.ORG_NAME_KEY, sharedPreferences.getString("ORG_NAME_KEY", "") ?: "")
            .putLong(
                User.PLANTER_CHECK_IN_ID_KEY,
                sharedPreferences.getLong("PLANTER_CHECK_IN_ID", -1)
            )
            .putLong(User.PLANTER_INFO_ID_KEY, sharedPreferences.getLong("PLANTER_INFO_ID", -1))
            .putString(
                User.PROFILE_PHOTO_PATH_KEY,
                sharedPreferences.getString("PLANTER_PHOTO", "")
            )
            .putLong(
                User.LAST_CHECK_IN_TIME_IN_KEY,
                sharedPreferences.getLong("TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS", -1)
            )
            .putBoolean(IS_MIGRATION_DONE_KEY, true)
            .commit()

        // Delete old system values
        sharedPreferences
            .edit()
            .remove("FIRST_NAME_KEY")
            .remove("FIRST_NAME_KEY")
            .remove("LAST_NAME_KEY")
            .remove("ORG_NAME_KEY")
            .remove("PLANTER_PHOTO")
            .remove("PLANTER_CHECK_IN_ID")
            .remove("PLANTER_INFO_ID")
            .remove("FIRST_RUN")
            .remove("TREE_TRACKER_SETTINGS_USED")
            .remove("TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS")
            .apply()
    }

    companion object {
        private val IS_MIGRATION_DONE_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("pref-migration")
    }
}
