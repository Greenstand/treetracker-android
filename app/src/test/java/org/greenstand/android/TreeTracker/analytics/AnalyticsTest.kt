/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.analytics

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnalyticsTest {
    private val firebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)
    private val loggedBundle = slot<Bundle>()
    private lateinit var analytics: Analytics

    @Before
    fun setUp() {
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "device-1234"
        every { DeviceUtils.language } returns "English"
        every { firebaseAnalytics.logEvent(any(), capture(loggedBundle)) } returns Unit
        analytics = Analytics(firebaseAnalytics, DeviceUtils)
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `creating analytics sets the device user properties`() {
        verify { firebaseAnalytics.setUserProperty("device_id", "device-1234") }
        verify { firebaseAnalytics.setUserProperty("device_language", "English") }
        verify { firebaseAnalytics.setUserProperty("app_version", BuildConfig.VERSION_NAME) }
        verify { firebaseAnalytics.setUserProperty("app_build", BuildConfig.VERSION_CODE.toString()) }
        verify { firebaseAnalytics.setUserProperty("manufacturer", Build.MANUFACTURER) }
        verify { firebaseAnalytics.setUserProperty("brand", Build.BRAND) }
        verify { firebaseAnalytics.setUserProperty("model", Build.MODEL) }
        verify { firebaseAnalytics.setUserProperty("hardware", Build.HARDWARE) }
        verify { firebaseAnalytics.setUserProperty("device", Build.DEVICE) }
        verify { firebaseAnalytics.setUserProperty("android_release", Build.VERSION.RELEASE) }
        verify { firebaseAnalytics.setUserProperty("sdk_version", Build.VERSION.SDK_INT.toString()) }
    }

    @Test
    fun `the device serial is never reported`() {
        verify { firebaseAnalytics.setUserProperty("serial", "unknown") }
    }

    @Test
    fun `tagScreen logs a screen view with the screen name and activity class`() {
        analytics.tagScreen(DashboardActivity(), "Dashboard")

        verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
        assertEquals("Dashboard", loggedBundle.captured.getString(FirebaseAnalytics.Param.SCREEN_NAME))
        assertEquals("DashboardActivity", loggedBundle.captured.getString(FirebaseAnalytics.Param.SCREEN_CLASS))
    }

    @Test
    fun `treePlanted logs an empty tree planted event`() {
        analytics.treePlanted()

        verify { firebaseAnalytics.logEvent(AnalyticEvents.TREE_PLANTED, any()) }
        assertTrue(loggedBundle.captured.isEmpty)
    }

    @Test
    fun `userCheckedIn logs an empty check in event`() {
        analytics.userCheckedIn()

        verify { firebaseAnalytics.logEvent(AnalyticEvents.USER_CHECK_IN, any()) }
        assertTrue(loggedBundle.captured.isEmpty)
    }

    @Test
    fun `userInfoCreated logs the phone and email`() {
        analytics.userInfoCreated(phone = "0712345678", email = "planter@example.com")

        verify { firebaseAnalytics.logEvent(AnalyticEvents.USER_INFO_CREATED, any()) }
        assertEquals("0712345678", loggedBundle.captured.getString("phone"))
        assertEquals("planter@example.com", loggedBundle.captured.getString("email"))
    }

    @Test
    fun `userEnteredDetails logs an empty event`() {
        analytics.userEnteredDetails()

        verify { firebaseAnalytics.logEvent(AnalyticEvents.USER_ENTERED_DETAILS, any()) }
        assertTrue(loggedBundle.captured.isEmpty)
    }

    @Test
    fun `userEnteredEmailPhone logs an empty event`() {
        analytics.userEnteredEmailPhone()

        verify { firebaseAnalytics.logEvent(AnalyticEvents.USER_ENTERED_EMAIL_PHONE, any()) }
        assertTrue(loggedBundle.captured.isEmpty)
    }

    @Test
    fun `treeNoteAdded logs the note length`() {
        analytics.treeNoteAdded(noteLength = 42)

        verify { firebaseAnalytics.logEvent(AnalyticEvents.NOTE_ADDED, any()) }
        assertEquals(42, loggedBundle.captured.getInt("note_length"))
    }

    @Test
    fun `syncButtonTapped logs the tree counts`() {
        analytics.syncButtonTapped(totalTrees = 10, treesSynced = 6, treesToSync = 4)

        verify { firebaseAnalytics.logEvent(AnalyticEvents.SYNC_BUTTON_CLICKED, any()) }
        assertTreeCounts(total = 10, synced = 6, unsynced = 4)
    }

    @Test
    fun `stopButtonTapped logs the tree counts`() {
        analytics.stopButtonTapped(totalTrees = 3, treesSynced = 1, treesToSync = 2)

        verify { firebaseAnalytics.logEvent(AnalyticEvents.STOP_BUTTON_CLICKED, any()) }
        assertTreeCounts(total = 3, synced = 1, unsynced = 2)
    }

    @Test
    fun `markerClicked logs the marker coordinates`() {
        analytics.markerClicked(lat = 37.42, long = -122.08)

        verify { firebaseAnalytics.logEvent(AnalyticEvents.MARKER_CLICKED, any()) }
        assertEquals(37.42, loggedBundle.captured.getDouble("lat"), 0.0)
        assertEquals(-122.08, loggedBundle.captured.getDouble("long"), 0.0)
    }

    @Test
    fun `uploadComplete logs nothing`() {
        analytics.uploadComplete(treesOnDevice = 5)

        verify(exactly = 0) { firebaseAnalytics.logEvent(any(), any()) }
    }

    private class DashboardActivity : Activity()

    private fun assertTreeCounts(
        total: Int,
        synced: Int,
        unsynced: Int,
    ) {
        assertEquals(total, loggedBundle.captured.getInt("total_trees"))
        assertEquals(synced, loggedBundle.captured.getInt("synced_trees"))
        assertEquals(unsynced, loggedBundle.captured.getInt("trees_unsynced"))
    }
}