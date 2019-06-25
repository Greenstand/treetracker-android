package org.greenstand.android.TreeTracker.analytics

import android.app.Activity
import android.os.Build
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.data.TreeColor
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

class Analytics(private val userManager: UserManager,
                private val firebaseAnalytics: FirebaseAnalytics,
                private val deviceUtils: DeviceUtils) {

    init {
        GlobalScope.launch {
            userManager.userLoginReceiveChannel.consumeEach {
                setupUserProperties()
            }
        }

        GlobalScope.launch {
            userManager.userDetailsReceiveChannel.consumeEach {
                setupUserProperties()
            }
        }
    }

    private fun setupUserProperties() {
        with(firebaseAnalytics) {
            // User
            setUserId(userManager.userId.toString())
            setUserProperty("first_name", userManager.firstName)
            setUserProperty("last_name", userManager.lastName)
            setUserProperty("organization", userManager.organization)

            // Device
            setUserProperty("device_id", deviceUtils.deviceId)
            setUserProperty("device_language", deviceUtils.language)
            setUserProperty("flavor", BuildConfig.FLAVOR)
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("app_build", BuildConfig.VERSION_CODE.toString())
            setUserProperty("manufacturer", Build.MANUFACTURER)
            setUserProperty("brand", Build.BRAND)
            setUserProperty("model", Build.MODEL)
            setUserProperty("hardware", Build.HARDWARE)
            setUserProperty("device", Build.DEVICE)
            setUserProperty("serial", Build.SERIAL)
            setUserProperty("android_release", Build.VERSION.RELEASE)
            setUserProperty("sdk_version", Build.VERSION.SDK_INT.toString())
        }
    }

    fun tagScreen(activty: Activity, screenName: String) {
        firebaseAnalytics.setCurrentScreen(activty, screenName, null)
    }

    fun uploadComplete(treesOnDevice: Int) {

    }

    fun treePlanted(treeInfo: String) {

    }

    fun userLoggedIn() {

    }

    fun userEnteredDetails() {

    }

    fun treeHeightMeasured(treeColor: TreeColor) {

    }

    fun treeNoteWritten(note: String) {

    }

    fun treeMarkerTapped(lat: Long, long: Long) {

    }

    fun syncButtonTapped(totalTrees: Int, treesSynced: Int, treesToSync: Int) {

    }
}