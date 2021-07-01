package org.greenstand.android.TreeTracker.models

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.preferences.UserPrefKey

enum class Language(val locale: Locale) {
    ENGLISH(Locale("en")),
    SWAHILI(Locale("sw"));

    companion object {

        fun fromString(lang: String): Language {
            return when (lang.toLowerCase()) {
                "en" -> ENGLISH
                "sw" -> SWAHILI
                else -> ENGLISH
            }
        }
    }
}

class LanguageSwitcher(private val prefs: Preferences) {

    fun applyCurrentLanguage(activity: Activity) {
        val language = Language.fromString(prefs.getString(LANGUAGE_PREF_KEY, "en") ?: "")
        setLanguage(language, activity.resources)
    }

    fun switch(activity: Activity) {
        val res = activity.resources
        val newLanguage = when (currentLanguage()) {
            Language.ENGLISH -> Language.SWAHILI
            Language.SWAHILI -> Language.ENGLISH
        }
        setLanguage(newLanguage, res)

        activity.finish()
        activity.startActivity(Intent(activity, MainActivity::class.java))
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

    fun currentLanguage(): Language {
        return Language.fromString(prefs.getString(LANGUAGE_PREF_KEY, "en") ?: "")
    }

    companion object {
        val LANGUAGE_PREF_KEY = PrefKeys.USER_SETTINGS + UserPrefKey("language")
    }
}
