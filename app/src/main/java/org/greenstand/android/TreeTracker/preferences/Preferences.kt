package org.greenstand.android.TreeTracker.preferences

import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.managers.UserManager

class Preferences(
    private val prefs: SharedPreferences,
    private val userManager: UserManager
) {

    fun getBoolean(prefKey: PrefKey, default: Boolean): Boolean {
        return prefs.getBoolean(computePath(prefKey), default)
    }

    fun getString(prefKey: PrefKey, default: String): String {
        return prefs.getString(computePath(prefKey), default)
    }

    fun getFloat(prefKey: PrefKey, default: Float): Float {
        return prefs.getFloat(computePath(prefKey), default)
    }

    fun getInt(prefKey: PrefKey, default: Int): Int {
        return prefs.getInt(computePath(prefKey), default)
    }

    fun computePath(prefKey: PrefKey): String {
        return when (prefKey) {
            is UserPrefKey -> {
                prefKey.path + "/${userManager.planterInfoId}"
            }
            else -> {
                prefKey.path
            }
        }
    }

    fun edit(): Editor {
        return Editor(prefs) {
            computePath(it)
        }
    }

    inner class Editor(
        prefs: SharedPreferences,
        private val computePath: (PrefKey) -> String
    ) {

        private var editor = prefs.edit()

        fun putBoolean(prefKey: PrefKey, value: Boolean) = apply {
            editor.putBoolean(computePath(prefKey), value)
        }

        fun putString(prefKey: PrefKey, value: String) = apply {
            editor.putString(computePath(prefKey), value)
        }

        fun putFloat(prefKey: PrefKey, value: Float) = apply {
            editor.putFloat(computePath(prefKey), value)
        }

        fun putInt(prefKey: PrefKey, value: Int) = apply {
            editor.putInt(computePath(prefKey), value)
        }

        fun apply() {
            editor.apply()
        }

        fun commit() {
            editor.commit()
        }
    }
}
