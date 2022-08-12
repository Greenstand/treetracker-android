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