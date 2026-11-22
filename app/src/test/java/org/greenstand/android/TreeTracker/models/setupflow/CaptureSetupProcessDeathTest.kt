/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.models.setupflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.orgRepoWith
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Process death during capture setup (#1418): Navigation 3 restores a setup screen such as Add
 * org, which asks [CaptureSetupScopeManager] for a scope nothing has opened in the new process.
 *
 * A new process is simulated by starting Koin again over the same SharedPreferences while the
 * manager holds no scope: that is what the restored screen finds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CaptureSetupProcessDeathTest {
    private val sharedPreferences =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getSharedPreferences("capture-setup-process-death-test", Context.MODE_PRIVATE)

    private val user =
        User(
            id = 7L,
            wallet = "5551230001",
            numberOfTrees = 3,
            firstName = "Ada",
            lastName = "Admin",
            photoPath = "/photos/ada.jpg",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    @Before
    fun setUp() {
        sharedPreferences.edit().clear().commit()
        CaptureSetupScopeManager.close()
        startProcess()
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    private fun startProcess() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<ExceptionDataCollector>(relaxed = true) }
                    single { orgRepoWith() }
                    single { mockk<StepCounter>(relaxed = true) }
                    single { mockk<SessionTracker>(relaxed = true) }
                    single { mockk<LocationDataCapturer>(relaxed = true) }
                    single { mockk<DeviceOrientation>(relaxed = true) }
                    single { Preferences(sharedPreferences) }
                    single { CaptureSetupStore(get()) }
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(get(), get<CaptureSetupStore>()) }
                        scoped { CaptureSetupNavigationController(get(), get(), get(), get(), get()) }
                    }
                },
            )
        }
    }

    /** The in-memory scope goes away with the process; what was written to disk stays. */
    private fun killProcess() {
        val saved = sharedPreferences.all.toMap()
        CaptureSetupScopeManager.close()
        sharedPreferences.edit().apply {
            clear()
            saved.forEach { (key, value) -> putString(key, value as String) }
            commit()
        }
        startProcess()
    }

    @Test
    fun `setup in progress comes back after process death`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().apply {
            user = this@CaptureSetupProcessDeathTest.user
            organizationName = "Kasiki Hai"
            sessionNote = "planted near the river"
            destinationWallet = "other-wallet"
        }

        killProcess()

        assertFalse(CaptureSetupScopeManager.isOpen)
        val restored = CaptureSetupScopeManager.getData()
        assertTrue(CaptureSetupScopeManager.isOpen)
        assertEquals(user, restored.user)
        assertEquals("Kasiki Hai", restored.organizationName)
        assertEquals("planted near the river", restored.sessionNote)
        assertEquals("other-wallet", restored.destinationWallet)
    }

    @Test
    fun `nav is available after process death`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user

        killProcess()

        assertNotNull(CaptureSetupScopeManager.nav)
        assertEquals(user, CaptureSetupScopeManager.getData().user)
    }

    @Test
    fun `changes made after the restore survive another process death`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user
        killProcess()
        CaptureSetupScopeManager.getData().organizationName = "Kasiki Hai"

        killProcess()

        val restored = CaptureSetupScopeManager.getData()
        assertEquals(user, restored.user)
        assertEquals("Kasiki Hai", restored.organizationName)
    }

    @Test
    fun `a closed setup is not restored`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user
        CaptureSetupScopeManager.close()

        killProcess()

        assertTrue(runCatching { CaptureSetupScopeManager.getData() }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { CaptureSetupScopeManager.nav }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `nothing is restored when no setup was started`() {
        killProcess()

        assertTrue(runCatching { CaptureSetupScopeManager.getData() }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `open after process death starts an empty setup`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user
        CaptureSetupScopeManager.getData().sessionNote = "old note"

        killProcess()
        CaptureSetupScopeManager.open()

        assertNull(CaptureSetupScopeManager.getData().user)
        assertNull(CaptureSetupScopeManager.getData().sessionNote)
    }

    @Test
    fun `unreadable saved setup is treated as none`() {
        CaptureSetupScopeManager.open()
        sharedPreferences.edit().apply {
            sharedPreferences.all.keys.forEach { putString(it, "not json") }
            commit()
        }

        killProcess()

        assertTrue(runCatching { CaptureSetupScopeManager.getData() }.exceptionOrNull() is IllegalStateException)
    }
}
