package org.greenstand.android.TreeTracker.viewmodels

import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.fragments.TermsPolicyFragment
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserManager

class TermsPolicyViewModel : CoroutineViewModel() {

    lateinit var userInfo: UserInfo

    fun confirm() {
        launch {
            //TreeManager.insertPlanter()
        }
    }

}