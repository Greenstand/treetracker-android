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

    fun putValue(config: Config, value: Any) {
        when(value) {
            is Boolean -> putBoolean(config as BooleanConfig, value)
            is Int -> putInt(config as IntConfig, value)
        }
    }

    fun getValue(config: Config): Any {
        return when(config) {
            is BooleanConfig -> getBoolean(config)
            is IntConfig -> getInt(config)
        }
    }
}