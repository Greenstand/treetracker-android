package org.greenstand.android.TreeTracker.walletselect.addwallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class AddWalletState(
    val walletName: String = "",
    val userImagePath: String = "",
)

class AddWalletViewModel(
    private val userId: Long,
    private val users: Users,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
) : ViewModel() {

    private val _state = MutableLiveData<AddWalletState>()
    val state: LiveData<AddWalletState> = _state

    init {
        viewModelScope.launch {
            _state.value = AddWalletState(
                userImagePath = users.getUser(userId)!!.photoPath
            )
        }
    }

    fun updateWalletName(destinationWallet: String) {
        _state.value = _state.value!!.copy(
            walletName = destinationWallet
        )
    }

    suspend fun startSession() {
        stepCounter.enable()
        sessionTracker.startSession(
            userId = userId,
            destinationWallet = _state.value!!.walletName,
            organization = "TEMP"
        )
        locationDataCapturer.startGpsUpdates()
    }
}

class AddWalletViewModelFactory(private val userId: Long)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddWalletViewModel(userId, get(), get(), get(), get()) as T
    }
}