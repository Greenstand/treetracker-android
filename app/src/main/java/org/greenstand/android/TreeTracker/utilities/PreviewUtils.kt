package org.greenstand.android.TreeTracker.utilities

import android.content.SharedPreferences

/**
 * A compilation of objects, classes, and interfaces to help stub out
 * functionality for custom PreviewParameters
 */
object PreviewUtils {

    val previewSharedPrefs = object : SharedPreferences {
        override fun getAll(): MutableMap<String, *> {
            TODO("Not yet implemented")
        }

        override fun getString(p0: String?, p1: String?): String? {
            TODO("Not yet implemented")
        }

        override fun getStringSet(p0: String?, p1: MutableSet<String>?): MutableSet<String> {
            TODO("Not yet implemented")
        }

        override fun getInt(p0: String?, p1: Int): Int {
            TODO("Not yet implemented")
        }

        override fun getLong(p0: String?, p1: Long): Long {
            TODO("Not yet implemented")
        }

        override fun getFloat(p0: String?, p1: Float): Float {
            TODO("Not yet implemented")
        }

        override fun getBoolean(p0: String?, p1: Boolean): Boolean {
            TODO("Not yet implemented")
        }

        override fun contains(p0: String?): Boolean {
            TODO("Not yet implemented")
        }

        override fun edit(): SharedPreferences.Editor {
            TODO("Not yet implemented")
        }

        override fun registerOnSharedPreferenceChangeListener(p0: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }

        override fun unregisterOnSharedPreferenceChangeListener(p0: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }
    }
}
