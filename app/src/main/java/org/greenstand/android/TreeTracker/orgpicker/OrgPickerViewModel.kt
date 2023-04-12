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
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo

data class OrgPickerState(
    val orgs: List<Org> = emptyList(),
    val currentOrg: Org? = null
)

class OrgPickerViewModel(
    private val orgRepo: OrgRepo,
) : ViewModel() {

    private val _state = MutableLiveData(OrgPickerState())
    val state: LiveData<OrgPickerState> = _state

    init {
        viewModelScope.launch {
            _state.value = OrgPickerState(
                orgs = orgRepo.getOrgs(),
                currentOrg = orgRepo.currentOrg()
            )
        }
    }

    fun setOrg(org: Org) {
        viewModelScope.launch {
            orgRepo.setOrg(org.id)
            _state.value = _state.value?.copy(
                currentOrg = orgRepo.currentOrg()
            )
        }
    }
}