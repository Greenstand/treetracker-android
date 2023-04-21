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
package org.greenstand.android.TreeTracker.languagepicker

import android.content.SharedPreferences
import android.content.res.Resources
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.preferences.Preferences

class LanguagePickerPreviewProvider : PreviewParameterProvider<LanguagePickerViewModel> {

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

    private val languageSwitcher = LanguageSwitcher(Preferences(stubSharedPrefs))

    override val values: Sequence<LanguagePickerViewModel> = sequenceOf(
        LanguagePickerViewModel(languageSwitcher, Resources.getSystem())
    )

    override val count: Int = values.count()
}