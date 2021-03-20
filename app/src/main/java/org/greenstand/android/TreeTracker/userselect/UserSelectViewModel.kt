package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.Users

class UserSelectViewModel(private val users: Users) : ViewModel() {

    private val _planterInfoList = MutableLiveData<List<PlanterInfoEntity>>()
    val planterInfoList: LiveData<List<PlanterInfoEntity>> = _planterInfoList

    init {
        viewModelScope.launch {
            _planterInfoList.value = users.getUsers()
        }
    }

}