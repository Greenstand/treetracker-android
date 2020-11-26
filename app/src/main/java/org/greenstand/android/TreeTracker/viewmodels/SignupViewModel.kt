package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.data.UserInfo

class SignupViewModel : ViewModel() {

    lateinit var userIdentification: String

    private val signupButtonStateMutableLiveData = MutableLiveData<Boolean>().apply {
        value = false
    }

    val signupButtonStateLiveDate: LiveData<Boolean> = signupButtonStateMutableLiveData

    var firstName: String = ""
        set(value) {
            field = value.trim()
            signupButtonStateMutableLiveData.value = firstName.isNotEmpty() && lastName.isNotEmpty()
        }

    var lastName: String = ""
        set(value) {
            field = value.trim()
            signupButtonStateMutableLiveData.value = firstName.isNotBlank() && lastName.isNotEmpty()
        }

    var organization: String = ""

    val userInfo: UserInfo
        get() = UserInfo(
            userIdentification,
            firstName,
            lastName,
            organization
        )
}