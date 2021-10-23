package org.greenstand.android.TreeTracker.languagepicker

import android.app.Activity
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher

class LanguagePickerViewModel(
    private val languageSwitcher: LanguageSwitcher,
    private val resource: Resources
) : ViewModel() {

    private val _currentLanguage = MutableLiveData(languageSwitcher.currentLanguage())
    val currentLanguage: LiveData<Language> = _currentLanguage

    init {
        languageSwitcher.observeCurrentLanguage()
            .onEach { _currentLanguage.value = it }
            .launchIn(viewModelScope)

    }

    fun setLanguage(language: Language) {
        languageSwitcher.setLanguage(language, resource)
    }

    fun refreshAppLanguage(activity: Activity) {
        languageSwitcher.applyCurrentLanguage(activity)
    }
}
