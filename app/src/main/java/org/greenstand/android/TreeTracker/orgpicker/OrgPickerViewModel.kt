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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class OrgPickerState(
    val orgs: List<Org> = emptyList(),
    val currentOrg: Org? = null,
)

sealed class OrgPickerAction : Action {
    data class SetOrg(
        val org: Org,
    ) : OrgPickerAction()

    object NavigateNext : OrgPickerAction()
}

class OrgPickerViewModel(
    private val orgRepo: OrgRepo,
) : BaseViewModel<OrgPickerState, OrgPickerAction>(OrgPickerState()) {
    init {
        viewModelScope.launch {
            val orgs = orgRepo.getOrgs()
            val currentOrg = orgRepo.currentOrg()
            updateState {
                copy(
                    orgs = orgs,
                    currentOrg = currentOrg,
                )
            }
        }
    }

    override fun handleAction(action: OrgPickerAction) {
        when (action) {
            is OrgPickerAction.SetOrg -> {
                viewModelScope.launch {
                    orgRepo.setOrg(action.org.id)
                    val currentOrg = orgRepo.currentOrg()
                    updateState { copy(currentOrg = currentOrg) }
                }
            }
            else -> { }
        }
    }
}