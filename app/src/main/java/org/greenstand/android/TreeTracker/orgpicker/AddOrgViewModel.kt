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
package org.greenstand.android.TreeTracker.orgpicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences


data class AddOrgState(
    val orgName: String = "",
    val previousOrgName: String? = null,
    val userImagePath: String = "",
)

class AddOrgViewModel(
    private val preferences: Preferences,
) : ViewModel() {

    private val _state = MutableLiveData<AddOrgState>()
    val state: LiveData<AddOrgState> = _state

    init {
        viewModelScope.launch {
            _state.value = AddOrgState(
                userImagePath = CaptureSetupScopeManager.getData().user!!.photoPath,
                previousOrgName = preferences.getString(PREV_ORG_KEY)
            )
        }
    }

    fun updateOrgName(orgName: String) {
        CaptureSetupScopeManager.getData().organizationName = orgName
        _state.value = _state.value!!.copy(
            orgName = orgName
        )
    }

    fun applyOrgAutofill() {
        _state.value = _state.value!!.copy(
            orgName = _state.value?.previousOrgName!!
        )
    }

    fun setDefaultOrg() {
        if (!_state.value?.orgName.isNullOrBlank()) {
            preferences.edit().putString(PREV_ORG_KEY, _state.value?.orgName).apply()
        }
        CaptureSetupScopeManager.getData().organizationName = _state.value?.orgName
    }

    companion object {
        private val PREV_ORG_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("autofill-org")
    }
}