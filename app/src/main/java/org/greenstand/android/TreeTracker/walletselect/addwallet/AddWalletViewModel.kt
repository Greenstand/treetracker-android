package org.greenstand.android.TreeTracker.walletselect.addwallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class AddWalletState(
    val walletName: String = "",
    val userImagePath: String = "",
)

class AddWalletViewModel(
    private val userId: Long,
    private val userRepo: UserRepo,
) : ViewModel() {

    private val _state = MutableLiveData<AddWalletState>()
    val state: LiveData<AddWalletState> = _state

    init {
        viewModelScope.launch {
            _state.value = AddWalletState(
                userImagePath = userRepo.getUser(userId)!!.photoPath
            )
        }
    }

    fun updateWalletName(destinationWallet: String) {
        _state.value = _state.value!!.copy(
            walletName = destinationWallet
        )
    }
}

class AddWalletViewModelFactory(private val userId: Long)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddWalletViewModel(userId, get()) as T
    }
}