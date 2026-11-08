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
package org.greenstand.android.TreeTracker.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugOverlayManagerTest {
    private val manager = DebugOverlayManager()

    @Test
    fun `overlays are hidden by default`() {
        assertFalse(manager.showSyncOverlay.value)
        assertFalse(manager.showSensorOverlay.value)
    }

    @Test
    fun `toggleSyncOverlay flips only the sync overlay`() {
        manager.toggleSyncOverlay()

        assertTrue(manager.showSyncOverlay.value)
        assertFalse(manager.showSensorOverlay.value)

        manager.toggleSyncOverlay()

        assertFalse(manager.showSyncOverlay.value)
    }

    @Test
    fun `toggleSensorOverlay flips only the sensor overlay`() {
        manager.toggleSensorOverlay()

        assertTrue(manager.showSensorOverlay.value)
        assertFalse(manager.showSyncOverlay.value)

        manager.toggleSensorOverlay()

        assertFalse(manager.showSensorOverlay.value)
    }

    @Test
    fun `setSyncOverlay sets the sync overlay visibility`() {
        manager.setSyncOverlay(true)
        assertTrue(manager.showSyncOverlay.value)

        manager.setSyncOverlay(true)
        assertTrue(manager.showSyncOverlay.value)

        manager.setSyncOverlay(false)
        assertFalse(manager.showSyncOverlay.value)
    }

    @Test
    fun `setSensorOverlay sets the sensor overlay visibility`() {
        manager.setSensorOverlay(true)
        assertTrue(manager.showSensorOverlay.value)
        assertFalse(manager.showSyncOverlay.value)

        manager.setSensorOverlay(false)
        assertFalse(manager.showSensorOverlay.value)
    }
}