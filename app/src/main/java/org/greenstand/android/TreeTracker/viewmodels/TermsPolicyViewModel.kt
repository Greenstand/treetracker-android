package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInParams
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoParams
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoUseCase

class TermsPolicyViewModel(
    private val createPlanterCheckInUseCase: CreatePlanterCheckInUseCase,
    private val createPlanterInfoUseCase: CreatePlanterInfoUseCase
) : ViewModel() {

    lateinit var userInfo: UserInfo
    var photoPath: String? = null
        set(value) {
            field = value
            if (!field.isNullOrEmpty()) {
                confirm(onNavigateToMap)
            }
        }

    var onNavigateToMap: () -> Unit = { }

    private fun confirm(onConfirmationComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {

            val planterInfoId = createPlanterInfoUseCase.execute(
                CreatePlanterInfoParams(
                    firstName = userInfo.firstName,
                    lastName = userInfo.lastName,
                    identifier = userInfo.identification,
                    organization = userInfo.organization,
                    phone = null,
                    email = null
                )
            )

            createPlanterCheckInUseCase.execute(
                CreatePlanterCheckInParams(
                    localPhotoPath = photoPath!!,
                    planterInfoId = planterInfoId
                )
            )

            withContext(Dispatchers.Main) { onConfirmationComplete() }
        }
    }
}
