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
package org.greenstand.android.TreeTracker.models

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import java.util.Locale

enum class Language(val locale: Locale) {
    ENGLISH(Locale("en")),
    SWAHILI(Locale("sw")),
    PORTUGUESE(Locale("pt"));

    companion object {

        fun fromString(lang: String): Language? {
            return when (lang.lowercase()) {
                "en" -> ENGLISH
                "sw" -> SWAHILI
                "pt" -> PORTUGUESE
                else -> null
            }
        }
    }
}

class LanguageSwitcher(private val prefs: Preferences) {

    fun applyCurrentLanguage(activity: Activity) {
        val language = Language.fromString(prefs.getString(LANGUAGE_PREF_KEY) ?: "")
        language?.also { language ->
            setLanguage(language, activity.resources)
        }
    }

    fun setLanguage(language: Language, res: Resources) {
        prefs.edit().putString(LANGUAGE_PREF_KEY, language.locale.toLanguageTag()).commit()

        val config = Configuration(res.configuration).apply {
            Locale.setDefault(language.locale)
            setLocale(language.locale)
        }

        res.updateConfiguration(
            config,
            res.displayMetrics
        )
    }

    fun currentLanguage(): Language? {
        return Language.fromString(prefs.getString(LANGUAGE_PREF_KEY) ?: "")
    }

    fun observeCurrentLanguage(): Flow<Language> {
        return prefs.observeString(LANGUAGE_PREF_KEY)
            .mapNotNull { langString -> langString?.let { Language.fromString(it) } }
    }

    companion object {
        val LANGUAGE_PREF_KEY = PrefKeys.USER_SETTINGS + PrefKey("language")
    }
}