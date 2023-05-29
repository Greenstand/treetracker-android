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
    val currentLanguage: LiveData<Language?> = _currentLanguage

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