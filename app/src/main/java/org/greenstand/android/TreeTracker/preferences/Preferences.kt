package org.greenstand.android.TreeTracker.preferences

import android.content.SharedPreferences

class Preferences(
    private val prefs: SharedPreferences
) {

    private var _planterInfoId: Long? = null

    fun setPlanterInfoId(planterInfoId: Long?) {
        _planterInfoId = planterInfoId
    }

    fun getBoolean(prefKey: PrefKey, default: Boolean): Boolean {
        return prefs.getBoolean(computePath(prefKey), default)
    }

    fun getString(prefKey: PrefKey, default: String? = null): String? {
        return prefs.getString(computePath(prefKey), default)
    }

    fun getLong(prefKey: PrefKey, default: Long = -1): Long {
        return prefs.getLong(computePath(prefKey), default)
    }

    fun getFloat(prefKey: PrefKey, default: Float = -1f): Float {
        return prefs.getFloat(computePath(prefKey), default)
    }

    fun getInt(prefKey: PrefKey, default: Int = -1): Int {
        return prefs.getInt(computePath(prefKey), default)
    }

    private fun computePath(prefKey: PrefKey): String {
        return when (prefKey) {
            is UserPrefKey -> prefKey.path + "/$_planterInfoId"
            else -> prefKey.path
        }
    }

    fun clearSessionData() {
        clearPrefKeyUsage(PrefKeys.SESSION)
    }

    private fun clearPrefKeyUsage(prefKey: PrefKey) {
        val editor = edit()
        prefs.all
            .keys
            .filter { prefKey.path in it }
            .forEach { editor.remove(PrefKey(it)) }
        editor.apply()
    }

    fun edit(): Editor {
        return Editor(prefs) {
            computePath(it)
        }
    }

    class Editor(
        prefs: SharedPreferences,
        private val computePath: (PrefKey) -> String
    ) {

        private var editor = prefs.edit()

        fun putBoolean(prefKey: PrefKey, value: Boolean) = apply {
            editor.putBoolean(computePath(prefKey), value)
        }

        fun putString(prefKey: PrefKey, value: String?) = apply {
            editor.putString(computePath(prefKey), value)
        }

        fun putLong(prefKey: PrefKey, value: Long) = apply {
            editor.putLong(computePath(prefKey), value)
        }

        fun putFloat(prefKey: PrefKey, value: Float) = apply {
            editor.putFloat(computePath(prefKey), value)
        }

        fun putInt(prefKey: PrefKey, value: Int) = apply {
            editor.putInt(computePath(prefKey), value)
        }

        fun remove(prefKey: PrefKey) = apply {
            editor.remove(computePath(prefKey))
        }

        fun apply() {
            editor.apply()
        }

        fun commit() {
            editor.commit()
        }
    }
}
