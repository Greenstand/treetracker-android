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
package org.greenstand.android.TreeTracker.devoptions

import org.greenstand.android.TreeTracker.preferences.Preferences

class Configurator constructor(
    private val prefs: Preferences,
) {
    fun getBoolean(config: BooleanConfig): Boolean {
        return prefs.getBoolean(config.key, config.defaultValue)
    }

    fun putBoolean(config: BooleanConfig, value: Boolean) {
        prefs.edit().putBoolean(config.key, value).apply()
    }

    fun getInt(config: IntConfig): Int {
        return prefs.getInt(config.key, config.defaultValue)
    }

    fun putInt(config: IntConfig, value: Int) {
        prefs.edit().putInt(config.key, value).apply()
    }

    fun getFloat(config: FloatConfig): Float {
        return prefs.getFloat(config.key, config.defaultValue)
    }

    fun putFloat(config: FloatConfig, value: Float) {
        prefs.edit().putFloat(config.key, value).apply()
    }

    fun putValue(config: Config, value: Any) {
        when (value) {
            is Boolean -> putBoolean(config as BooleanConfig, value)
            is Int -> putInt(config as IntConfig, value)
            is Float -> putFloat(config as FloatConfig, value)
        }
    }

    fun getValue(config: Config): Any {
        return when (config) {
            is BooleanConfig -> getBoolean(config)
            is IntConfig -> getInt(config)
            is FloatConfig -> getFloat(config)
        }
    }
}