package org.greenstand.android.TreeTracker.splash

import android.content.SharedPreferences
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenPreviewProvider : PreviewParameterProvider<SplashScreenViewModel> {

    private val stubSharedPrefs = object : SharedPreferences {
        override fun getAll(): MutableMap<String, *> {
            TODO("Not yet implemented")
        }

        override fun getString(key: String?, defValue: String?): String? {
            TODO("Not yet implemented")
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> {
            TODO("Not yet implemented")
        }

        override fun getInt(key: String?, defValue: Int): Int {
            TODO("Not yet implemented")
        }

        override fun getLong(key: String?, defValue: Long): Long {
            TODO("Not yet implemented")
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            TODO("Not yet implemented")
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            TODO("Not yet implemented")
        }

        override fun contains(key: String?): Boolean {
            TODO("Not yet implemented")
        }

        override fun edit(): SharedPreferences.Editor {
            TODO("Not yet implemented")
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }
    }

    override val values: Sequence<SplashScreenViewModel> = sequenceOf(
        SplashScreenViewModel(PreferencesMigrator(stubSharedPrefs, preferences = Preferences(stubSharedPrefs)))
    )

    override val count: Int = values.count()
}
