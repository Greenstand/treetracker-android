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
package org.greenstand.android.TreeTracker.uitest.devoptions

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.devoptions.DevOptionsRoot
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module

class DevOptionsRootUiTest : RootScreenUiTest() {
    private val configurator = mockk<Configurator>(relaxed = true)
    private val convergenceConfiguration = mockk<ConvergenceConfiguration>(relaxed = true)
    private val overlayManager = DebugOverlayManager()

    @Before
    fun setUp() {
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
        startKoinWith(
            module {
                single { viewModel }
                single { overlayManager }
            },
        )
        backStack.add(DevOptionsRoute)
        setRootContent { DevOptionsRoot() }
    }

    @Test
    fun `back pops dev options`() {
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `sync overlay switch drives the injected overlay manager`() {
        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))[0].performClick()

        assertTrue(overlayManager.showSyncOverlay.value)
    }

    @Test
    fun `checking a boolean param stores it`() {
        composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()

        verify { configurator.putValue(ConfigKeys.FORCE_IMAGE_SIZE.copy(defaultValue = false), true) }
    }
}