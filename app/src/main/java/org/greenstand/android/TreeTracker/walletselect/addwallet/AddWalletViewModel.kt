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
package org.greenstand.android.TreeTracker.walletselect.addwallet

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class AddWalletState(
    val walletName: String = "",
    val userImagePath: String = "",
    /** True while the screen, restored after process death, reloads its setup user. Next and Back wait. */
    val restoring: Boolean = false,
    /** True when the screen came back after process death and its user could not be found again. */
    val setupLost: Boolean = false,
)

sealed class AddWalletAction : Action {
    data class UpdateWalletName(
        val destinationWallet: String,
    ) : AddWalletAction()

    object NavigateBack : AddWalletAction()

    object NavigateNext : AddWalletAction()
}

class AddWalletViewModel(
    private val restorer: CaptureSetupRestorer,
) : BaseViewModel<AddWalletState, AddWalletAction>(AddWalletState()) {
    init {
        // After process death Nav3 restores this screen with the capture setup scope closed (#1447).
        val restored = restorer.reopenScopeIfClosed()
        if (restored) updateState { copy(restoring = true) }
        viewModelScope.launch {
            val user = restorer.setupUser(restored)
            updateState {
                copy(
                    restoring = false,
                    setupLost = restored && user == null,
                    userImagePath = user?.photoPath ?: "",
                )
            }
        }
    }

    override fun handleAction(action: AddWalletAction) {
        // The setup scope is closed once setup is lost; the screen is on its way to the Dashboard.
        if (currentState.setupLost) return
        when (action) {
            is AddWalletAction.UpdateWalletName -> {
                CaptureSetupScopeManager.getData().destinationWallet = action.destinationWallet
                updateState { copy(walletName = action.destinationWallet) }
            }
            else -> { }
        }
    }
}