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
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class AddOrgState(
    val orgName: String = "",
    val previousOrgName: String? = null,
    val userImagePath: String = "",
)

sealed class AddOrgAction : Action {
    data class UpdateOrgName(
        val orgName: String,
    ) : AddOrgAction()

    object ApplyOrgAutofill : AddOrgAction()

    object SetDefaultOrg : AddOrgAction()

    object NavigateBack : AddOrgAction()

    object NavigateNext : AddOrgAction()
}

class AddOrgViewModel(
    private val preferences: Preferences,
    private val orgRepo: OrgRepo,
) : BaseViewModel<AddOrgState, AddOrgAction>(AddOrgState()) {
    init {
        viewModelScope.launch {
            val currentOrgName =
                orgRepo.currentOrg().name
                    .takeIf { it.isNotBlank() && it != "Greenstand" }
            updateState {
                copy(
                    userImagePath = CaptureSetupScopeManager.getData().user!!.photoPath,
                    previousOrgName = preferences.getString(PREV_ORG_KEY),
                    orgName = currentOrgName ?: "",
                )
            }
            if (currentOrgName != null) {
                CaptureSetupScopeManager.getData().organizationName = currentOrgName
            }
        }
    }

    override fun handleAction(action: AddOrgAction) {
        when (action) {
            is AddOrgAction.UpdateOrgName -> {
                CaptureSetupScopeManager.getData().organizationName = action.orgName
                updateState { copy(orgName = action.orgName) }
            }
            is AddOrgAction.ApplyOrgAutofill -> {
                updateState { copy(orgName = previousOrgName!!) }
            }
            is AddOrgAction.SetDefaultOrg -> {
                if (!currentState.orgName.isBlank()) {
                    preferences.edit().putString(PREV_ORG_KEY, currentState.orgName).apply()
                }
                CaptureSetupScopeManager.getData().organizationName = currentState.orgName
            }
            else -> { }
        }
    }

    companion object {
        private val PREV_ORG_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("autofill-org")
    }
}