package org.greenstand.android.TreeTracker.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInParams
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoParams
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoUseCase

class TermsPolicyViewModel(private val createPlanterCheckInUseCase: CreatePlanterCheckInUseCase,
                           private val createPlanterInfoUseCase: CreatePlanterInfoUseCase) : CoroutineViewModel() {

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
        launch(Dispatchers.IO) {

            createPlanterInfoUseCase.execute(CreatePlanterInfoParams(firstName = userInfo.firstName,
                                                                     lastName = userInfo.lastName,
                                                                     identifier = userInfo.identification,
                                                                     organization = userInfo.organization,
                                                                     phone = null,
                                                                     email = null))

            createPlanterCheckInUseCase.execute(CreatePlanterCheckInParams(localPhotoPath = photoPath!!,
                                                                           identifier = userInfo.identification))



            withContext(Dispatchers.Main) { onConfirmationComplete() }
        }
    }

}