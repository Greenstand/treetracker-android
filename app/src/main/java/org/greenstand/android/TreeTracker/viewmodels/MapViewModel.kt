package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.ExpireCheckInStatusUseCase
import org.greenstand.android.TreeTracker.usecases.ValidateCheckInStatusUseCase

class MapViewModel constructor(private val validateCheckInStatusUseCase: ValidateCheckInStatusUseCase,
                               private val expireCheckInStatusUseCase: ExpireCheckInStatusUseCase,
                               private val createFakeTreesUseCase: CreateFakeTreesUseCase,
                               private val userManager: UserManager) : ViewModel() {

    val checkInStatusLiveData = MutableLiveData<Boolean>()

    suspend fun checkForValidUser() {
        if (validateCheckInStatusUseCase.execute(Unit)) {
            checkInStatusLiveData.postValue(true)
        } else {
            expireCheckInStatusUseCase.execute(Unit)
            checkInStatusLiveData.postValue(false)
        }
    }

    fun getPlanterName(): String {
        return userManager.firstName + " " + userManager.lastName
    }

    suspend fun createFakeTrees(): Boolean {
        createFakeTreesUseCase.execute(CreateFakeTreesParams(500))
        return true
    }

}