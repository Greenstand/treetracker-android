package org.greenstand.android.TreeTracker.languagepicker

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher

class LanguagePickerViewModel(
    private val languageSwitcher: LanguageSwitcher,
    private val resource: Resources
    ) : ViewModel() {

    private val _currentLanguage = MutableLiveData(languageSwitcher.currentLanguage())
    val currentLanguage: LiveData<Language> = _currentLanguage

    fun setLanguage(language: Language) {
        languageSwitcher.setLanguage(language, resource)
        _currentLanguage.value = language
    }

    fun refreshAppLanguage(activity: Activity) {
        languageSwitcher.applyCurrentLanguage(activity)
    }
}