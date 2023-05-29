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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

data class AddWalletState(
    val walletName: String = "",
    val userImagePath: String = "",
)

class AddWalletViewModel : ViewModel() {

    private val _state = MutableLiveData<AddWalletState>()
    val state: LiveData<AddWalletState> = _state

    init {
        viewModelScope.launch {
            _state.value = AddWalletState(
                userImagePath = CaptureSetupScopeManager.getData().user!!.photoPath
            )
        }
    }

    fun updateWalletName(destinationWallet: String) {
        CaptureSetupScopeManager.getData().destinationWallet = destinationWallet
        _state.value = _state.value!!.copy(
            walletName = destinationWallet
        )
    }
}