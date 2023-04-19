/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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