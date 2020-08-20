package org.greenstand.android.TreeTracker.managers

import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LocationDataCapturerTest {

    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var userManager: UserManager
    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager
    @MockK(relaxed = true)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferences: Preferences
    @MockK(relaxed = true)
    private lateinit var treeTrackerDAO: TreeTrackerDAO

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        preferences = Preferences(sharedPreferences, userManager)
        locationDataCapturer = LocationDataCapturer(
            userManager,
            locationUpdateManager,
            preferences,
            treeTrackerDAO
        )
    }

    @Test
    fun start() {
        val liveData = mockk<LiveData<Location?>>(relaxUnitFun = true)
        every { locationUpdateManager.locationUpdateLiveData } returns liveData

        locationDataCapturer.start()

        verify { liveData.observeForever(any<Observer<Location?>>()) }
    }

    @Test
    fun turnOnTreeCaptureMode() {
        val key = PrefKeys.SESSION + PrefKey(ValueHelper.CURRENT_TREE_ID)

        locationDataCapturer.turnOnTreeCaptureMode()

        assertNotNull(locationDataCapturer.generatedTreeUuid)
    }

    @Test
    fun turnOffTreeCaptureMode() {
        val key = PrefKeys.SESSION + PrefKey(ValueHelper.CURRENT_TREE_ID)

        locationDataCapturer.turnOffTreeCaptureMode()

        assertNull(locationDataCapturer.generatedTreeUuid)
    }
}
