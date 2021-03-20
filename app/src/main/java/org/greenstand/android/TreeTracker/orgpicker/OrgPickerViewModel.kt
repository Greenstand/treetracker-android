package org.greenstand.android.TreeTracker.orgpicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Org
import org.greenstand.android.TreeTracker.models.Organizations

data class OrgPickerState(
    val orgs: List<Org> = emptyList(),
    val currentOrg: Org? = null
)

sealed class OrgPickerUIEvents {
    object Exit : OrgPickerUIEvents()
}

class OrgPickerViewModel(
    private val organizations: Organizations,
) : ViewModel() {

    private val _state = MutableLiveData(OrgPickerState())
    val state: LiveData<OrgPickerState> = _state

    private val _uiEvents = MutableLiveData<OrgPickerUIEvents>()
    val uiEvents: LiveData<OrgPickerUIEvents> = _uiEvents

    init {
        viewModelScope.launch {
            _state.value = OrgPickerState(
                orgs = organizations.getOrgs(),
                currentOrg = organizations.getCurrentOrg()
            )
        }
    }

    fun setOrg(org: Org) {
        viewModelScope.launch {
            organizations.setCurrentOrg(org)
            _uiEvents.value = OrgPickerUIEvents.Exit
        }
    }
}