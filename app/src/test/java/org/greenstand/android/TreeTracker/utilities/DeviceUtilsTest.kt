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
package org.greenstand.android.TreeTracker.utilities

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceUtilsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        TreeTrackerApplication.appContext = application
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `deviceId is the android id`() {
        Settings.Secure.putString(application.contentResolver, Settings.Secure.ANDROID_ID, "device-1234")

        assertEquals("device-1234", DeviceUtils.deviceId)
    }

    @Test
    fun `language is the display name of the default locale language`() {
        Locale.setDefault(Locale.ENGLISH)

        assertEquals("English", DeviceUtils.language)
    }

    @Test
    fun `language follows the default locale`() {
        Locale.setDefault(Locale.FRENCH)

        assertEquals(Locale.FRENCH.displayLanguage, DeviceUtils.language)
    }
}