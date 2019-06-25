package org.greenstand.android.TreeTracker.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.Utils
import java.util.*

class TermsPolicyViewModel(private val userManager: UserManager,
                           private val userLocationManager: UserLocationManager) : CoroutineViewModel() {

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

            userManager.login(userInfo.identification,
                              photoPath!!,
                              userLocationManager.currentLocation)

            userManager.addLoginDetails(userInfo.identification,
                                        userInfo.firstName,
                                        userInfo.lastName,
                                        userInfo.organization,
                                        Utils.dateFormat.format(Date()),
                                        userLocationManager.currentLocation)

            withContext(Dispatchers.Main) { onConfirmationComplete() }
        }
    }

}