package org.greenstand.android.TreeTracker.analytics

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.MARKER_CLICKED
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.NOTE_ADDED
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.STOP_BUTTON_CLICKED
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.SYNC_BUTTON_CLICKED
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.TREE_PLANTED
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.USER_CHECK_IN
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.USER_ENTERED_DETAILS
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.USER_ENTERED_EMAIL_PHONE
import org.greenstand.android.TreeTracker.analytics.AnalyticEvents.USER_INFO_CREATED
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

class Analytics(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val deviceUtils: DeviceUtils
) {

    init {
        setupStaticDeviceProperties()
    }

    private fun setupStaticDeviceProperties() {
        with(firebaseAnalytics) {
            setUserProperty("device_id", deviceUtils.deviceId)
            setUserProperty("device_language", deviceUtils.language)
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

    fun treePlanted() {
        firebaseAnalytics.logEvent(TREE_PLANTED, Bundle())
    }

    fun userCheckedIn() {
        firebaseAnalytics.logEvent(USER_CHECK_IN, Bundle())
    }

    fun userInfoCreated(phone: String, email: String) {
        val bundle = Bundle().apply {
            putString("email", email)
            putString("phone", phone)
        }
        firebaseAnalytics.logEvent(USER_INFO_CREATED, bundle)
    }

    fun userEnteredDetails() {
        firebaseAnalytics.logEvent(USER_ENTERED_DETAILS, Bundle())
    }

    fun userEnteredEmailPhone() {
        firebaseAnalytics.logEvent(USER_ENTERED_EMAIL_PHONE, Bundle())
    }

    fun treeNoteAdded(noteLength: Int) {
        val bundle = Bundle().apply {
            putInt("note_length", noteLength)
        }
        firebaseAnalytics.logEvent(NOTE_ADDED, bundle)
    }

    fun syncButtonTapped(totalTrees: Int, treesSynced: Int, treesToSync: Int) {
        val bundle = Bundle().apply {
            putInt("total_trees", totalTrees)
            putInt("synced_trees", treesSynced)
            putInt("trees_unsynced", treesToSync)
        }
        firebaseAnalytics.logEvent(SYNC_BUTTON_CLICKED, bundle)
    }

    fun stopButtonTapped(totalTrees: Int, treesSynced: Int, treesToSync: Int) {
        val bundle = Bundle().apply {
            putInt("total_trees", totalTrees)
            putInt("synced_trees", treesSynced)
            putInt("trees_unsynced", treesToSync)
        }
        firebaseAnalytics.logEvent(STOP_BUTTON_CLICKED, bundle)
    }

    fun markerClicked(lat: Double, long: Double) {
        val bundle = Bundle().apply {
            putDouble("lat", lat)
            putDouble("long", long)
        }
        firebaseAnalytics.logEvent(MARKER_CLICKED, bundle)
    }
}
