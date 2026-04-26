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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class LanguagePickerState(
    val currentLanguage: Language? = null,
)

sealed class LanguagePickerAction : Action {
    data class SetLanguage(
        val language: Language,
    ) : LanguagePickerAction()

    object ConfirmLanguage : LanguagePickerAction()

    object NavigateNext : LanguagePickerAction()

    object ResetNavigation : LanguagePickerAction()
}

class LanguagePickerViewModel(
    private val languageSwitcher: LanguageSwitcher,
) : BaseViewModel<LanguagePickerState, LanguagePickerAction>(LanguagePickerState(currentLanguage = languageSwitcher.currentLanguage())) {
    private var hasNavigated = false

    init {
        languageSwitcher
            .observeCurrentLanguage()
            .onEach { updateState { copy(currentLanguage = it) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LanguagePickerAction) {
        when (action) {
            is LanguagePickerAction.SetLanguage -> {
                updateState { copy(currentLanguage = action.language) }
            }
            is LanguagePickerAction.ConfirmLanguage -> {
                state.value.currentLanguage?.let { languageSwitcher.setLanguage(it) }
            }
            is LanguagePickerAction.NavigateNext -> {
                // handled in composable, guarded here
            }
            is LanguagePickerAction.ResetNavigation -> { // ← ADD
                hasNavigated = false
            }
        }
    }

    fun tryNavigate(): Boolean {
        if (hasNavigated) return false
        hasNavigated = true
        return true
    }
}