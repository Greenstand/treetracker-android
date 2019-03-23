package org.greenstand.android.TreeTracker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper.NAME_SPACE
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object SharedPrefsManager {

    private val sharedPrefs by lazy {
        TreeTrackerApplication.appContext().getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean by Preference(sharedPrefs, ValueHelper.FIRST_RUN, true)

    var areSettingsUsed: Boolean by Preference(sharedPrefs, ValueHelper.TREE_TRACKER_SETTINGS_USED, true)

    var planterIdentifier: String? by Preference(sharedPrefs, ValueHelper.PLANTER_IDENTIFIER, null)

    var lastTimeUserIdentified: Long by Preference(sharedPrefs, ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)

    var planterPhoto: String? by Preference(sharedPrefs, ValueHelper.PLANTER_PHOTO, null)

    var treesToBeDownloadedFirst: Boolean by Preference(sharedPrefs, ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)

    var planterId: Long by Preference(sharedPrefs, ValueHelper.PLANTER_IDENTIFIER_ID, 0)

    var minAccuracyGlobalSetting: Int by Preference(sharedPrefs, ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 0)

    var timeToNextUpdateGlobalSetting: Int by Preference(sharedPrefs, ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, 0)

    var saveAndEdit: Boolean by Preference(sharedPrefs, ValueHelper.SAVE_AND_EDIT, false)

}


class Preference<T>(private val prefs: SharedPreferences,
                    private val name: String,
                    private val default: T): ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return findPreference(name, default)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putPreference(name, value)
    }

    private fun <T> findPreference(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException("This type cannot be saved into Preferences")
        }
        @Suppress("UNCHECKED_CAST")
        res as T
    }

    @SuppressLint("CommitPrefEdits")
    private fun <T> putPreference(name: String, value: T) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type cannot be saved into Preferences")
        }.apply()
    }
}