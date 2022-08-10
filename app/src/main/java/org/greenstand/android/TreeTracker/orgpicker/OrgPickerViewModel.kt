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