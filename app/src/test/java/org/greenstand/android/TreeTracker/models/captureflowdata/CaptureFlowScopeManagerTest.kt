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
package org.greenstand.android.TreeTracker.models.captureflowdata

import io.mockk.mockk
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.orgRepoWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CaptureFlowScopeManagerTest {
    private val stepCounter = mockk<StepCounter>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val deviceOrientation = mockk<DeviceOrientation>(relaxed = true)

    @Before
    fun setUp() {
        CaptureFlowScopeManager.close()
        stopKoin()
        startKoin {
            modules(
                module {
                    single { orgRepoWith() }
                    single { stepCounter }
                    single { sessionTracker }
                    single { locationDataCapturer }
                    single { deviceOrientation }
                    scope<CaptureFlowScope> {
                        scoped { CaptureFlowData() }
                        scoped { mockk<TreeCapturer>(relaxed = true) }
                        scoped { CaptureFlowNavigationController(get(), get(), get(), get(), get(), get()) }
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        CaptureFlowScopeManager.close()
        stopKoin()
    }

    @Test
    fun `is closed before open is called`() {
        assertFalse(CaptureFlowScopeManager.isOpen)
    }

    @Test
    fun `getData before open throws`() {
        val error = runCatching { CaptureFlowScopeManager.getData() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("CaptureFlowScope not open. Call open() before getData().", error?.message)
    }

    @Test
    fun `nav before open throws`() {
        val error = runCatching { CaptureFlowScopeManager.nav }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("CaptureFlowScope not open. Call open() before accessing nav.", error?.message)
    }

    @Test
    fun `open makes the scope available`() {
        CaptureFlowScopeManager.open()

        assertTrue(CaptureFlowScopeManager.isOpen)
    }

    @Test
    fun `getData returns the scope component that owns the open scope`() {
        CaptureFlowScopeManager.open()

        val component = CaptureFlowScopeManager.getData()

        assertSame(component, CaptureFlowScopeManager.getData())
        assertSame(component.scope.get<CaptureFlowData>(), component.scope.get<CaptureFlowData>())
    }

    @Test
    fun `nav returns the same controller while the scope is open`() {
        CaptureFlowScopeManager.open()

        assertSame(CaptureFlowScopeManager.nav, CaptureFlowScopeManager.nav)
    }

    @Test
    fun `reopening starts a fresh scope`() {
        CaptureFlowScopeManager.open()
        val firstComponent = CaptureFlowScopeManager.getData()
        val firstData = firstComponent.scope.get<CaptureFlowData>()
        val firstNav = CaptureFlowScopeManager.nav

        CaptureFlowScopeManager.open()

        assertTrue(CaptureFlowScopeManager.isOpen)
        assertNotSame(firstComponent, CaptureFlowScopeManager.getData())
        assertNotSame(firstData, CaptureFlowScopeManager.getData().scope.get<CaptureFlowData>())
        assertNotSame(firstNav, CaptureFlowScopeManager.nav)
    }

    @Test
    fun `close makes the scope unavailable`() {
        CaptureFlowScopeManager.open()

        CaptureFlowScopeManager.close()

        assertFalse(CaptureFlowScopeManager.isOpen)
        assertTrue(runCatching { CaptureFlowScopeManager.nav }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `close when not open is a no-op`() {
        CaptureFlowScopeManager.close()
        CaptureFlowScopeManager.close()

        assertFalse(CaptureFlowScopeManager.isOpen)
    }
}