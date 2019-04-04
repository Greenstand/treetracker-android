package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.Validation.isEmailValid

class PlanterDetailsViewModel: CoroutineViewModel()  {

    fun isUserPresentOnDevice(identifier: String): LiveData<PlanterDetailsEntity>{
        return TreeManager.getPlanterByInputtedText(identifier)
    }
}