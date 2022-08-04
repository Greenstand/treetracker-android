package org.greenstand.android.TreeTracker.orgpicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.get


data class AddOrgState(
    val orgName: String = "",
    val previousOrgName: String? = null,
    val userImagePath: String = "",
)

class AddOrgViewModel(
    private val userId: Long,
    private val destinationWallet: String,
    private val userRepo: UserRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
    private val preferences: Preferences,
) : ViewModel() {

    private val _state = MutableLiveData<AddOrgState>()
    val state: LiveData<AddOrgState> = _state

    init {
        viewModelScope.launch {
            _state.value = AddOrgState(
                userImagePath = userRepo.getUser(userId)!!.photoPath,
                previousOrgName = preferences.getString(PREV_ORG_KEY)
            )
        }
    }

    fun updateOrgName(orgName: String) {
        _state.value = _state.value!!.copy(
            orgName = orgName
        )
    }

    fun applyOrgAutofill() {
        _state.value = _state.value!!.copy(
            orgName = _state.value?.previousOrgName!!
        )
    }

    suspend fun startSession() {
        if (!_state.value?.orgName.isNullOrBlank()) {
            preferences.edit().putString(PREV_ORG_KEY, _state.value?.orgName).apply()
        }
        stepCounter.enable()
        sessionTracker.startSession(
            userId = userId,
            destinationWallet = destinationWallet,
            organization = _state.value!!.orgName
        )
        locationDataCapturer.startGpsUpdates()
    }

    companion object {
        private val PREV_ORG_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("autofill-org")
    }
}

class AddOrgViewModelFactory(
    private val userId: Long,
    private val destinationWallet: String)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddOrgViewModel(userId, destinationWallet, get(), get(), get(), get(), get()) as T
    }
}