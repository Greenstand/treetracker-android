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
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class AddOrgState(
    val orgName: String = "",
    val previousOrgName: String? = null,
    val userImagePath: String = "",
    /** True when Add org came back after process death and its user could not be found again. */
    val setupLost: Boolean = false,
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
    private val userRepo: UserRepo,
) : BaseViewModel<AddOrgState, AddOrgAction>(AddOrgState()) {
    init {
        // After process death Nav3 restores Add org, but the capture setup scope only lived in
        // memory. Reopen it at this step and reload the user saved below before reading it.
        val restoring = !CaptureSetupScopeManager.isOpen
        if (restoring) {
            CaptureSetupScopeManager.open()
            CaptureSetupScopeManager.nav.restoreAt(RouteRegistry.ROUTE_ADD_ORG)
        }
        viewModelScope.launch {
            val user =
                if (restoring) {
                    userRepo
                        .getUser(preferences.getLong(SETUP_USER_ID_KEY))
                        ?.also { CaptureSetupScopeManager.getData().user = it }
                } else {
                    CaptureSetupScopeManager.getData().user
                }
            if (user == null) {
                CaptureSetupScopeManager.close()
                updateState { copy(setupLost = true) }
                return@launch
            }
            preferences.edit().putLong(SETUP_USER_ID_KEY, user.id).apply()
            val currentOrgName =
                orgRepo
                    .currentOrg()
                    .name
                    .takeIf { it.isNotBlank() && it != "Greenstand" }
            updateState {
                copy(
                    userImagePath = user.photoPath,
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

        /** The capture setup user, kept on disk so Add org can be restored after process death. */
        internal val SETUP_USER_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("add-org-user-id")
    }
}