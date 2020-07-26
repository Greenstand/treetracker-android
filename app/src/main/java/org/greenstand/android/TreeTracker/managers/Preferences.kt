package org.greenstand.android.TreeTracker.managers

import android.content.SharedPreferences
import timber.log.Timber

object PrefKeys {

    val ROOT = PrefKey("greenstand")

    val SESSION = PrefKey("session")

    val USER_SETTINGS = ROOT + PrefKey("user-settings")
}

open class PrefKey(val path: String) {

    operator fun plus(prefKey: PrefKey): PrefKey {
        return when (prefKey) {
            is UserPrefKey -> UserPrefKey("$path/${prefKey.path}")
            else -> PrefKey("$path/${prefKey.path}")
        }
    }

    fun asUserPref(): UserPrefKey {
        return UserPrefKey(path)
    }
}

class UserPrefKey(path: String) : PrefKey(path)

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
        }.also { Timber.d("JONATHAN $it") }
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
