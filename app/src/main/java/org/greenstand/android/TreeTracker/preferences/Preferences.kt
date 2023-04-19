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
package org.greenstand.android.TreeTracker.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class Preferences(
    private val prefs: SharedPreferences
) {

    private val prefUpdateFlow: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    private val preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            prefUpdateFlow.tryEmit(key)
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

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

    fun observeInt(key: PrefKey, default: Int = -1): Flow<Int> {
        return prefUpdateFlow.filter { it == computePath(key) }
            .map { getInt(key, default) }
    }

    fun observeString(key: PrefKey, default: String? = null): Flow<String?> {
        return prefUpdateFlow.filter { it == computePath(key) }
            .map { getString(key, default) }
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