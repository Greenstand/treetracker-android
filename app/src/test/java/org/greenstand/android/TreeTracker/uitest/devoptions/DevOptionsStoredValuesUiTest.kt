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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.greenstand.android.TreeTracker.devoptions.BooleanConfig
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.devoptions.DevOptionsRoot
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.devoptions.FloatConfig
import org.greenstand.android.TreeTracker.devoptions.IntConfig
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module

/** Tests for [DevOptionsRoot] showing and changing the values already stored in the [Configurator]. */
class DevOptionsStoredValuesUiTest : RootScreenUiTest() {
    private val configurator = mockk<Configurator>(relaxed = true)
    private val overlayManager = DebugOverlayManager()

    @Before
    fun setUp() {
        every { configurator.getBoolean(any()) } answers { firstArg<BooleanConfig>().defaultValue }
        every { configurator.getInt(any()) } answers { firstArg<IntConfig>().defaultValue }
        every { configurator.getFloat(any()) } answers { firstArg<FloatConfig>().defaultValue }
        every { configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE) } returns true
        every { configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT) } returns STORED_HEIGHT

        val viewModel = DevOptionsViewModel(configurator, mockk<ConvergenceConfiguration>(relaxed = true))
        startKoinWith(
            module {
                single { viewModel }
                single { overlayManager }
            },
        )
        backStack.add(DevOptionsRoute)
        setRootContent { DevOptionsRoot() }
    }

    private fun checkbox() = composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))

    private fun switches() = composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))

    private fun heightField() = composeTestRule.onNode(hasSetTextAction() and hasText(STORED_HEIGHT.toString()))

    private val storedForceImageSize = ConfigKeys.FORCE_IMAGE_SIZE.copy(defaultValue = true)
    private val storedHeight = ConfigKeys.IMAGE_CAPTURE_HEIGHT.copy(defaultValue = STORED_HEIGHT)

    @Test
    fun `stored values are shown instead of the defaults`() {
        checkbox().assertIsOn()
        heightField().assertExists()
        composeTestRule.onNodeWithText(ConfigKeys.IMAGE_CAPTURE_HEIGHT.defaultValue.toString()).assertDoesNotExist()
    }

    @Test
    fun `unchecking a stored boolean stores false`() {
        checkbox().performClick()

        checkbox().assertIsOff()
        verify { configurator.putValue(storedForceImageSize, false) }
    }

    @Test
    fun `checking a boolean twice stores false and then true`() {
        checkbox().performClick()
        checkbox().performClick()

        checkbox().assertIsOn()
        verifyOrder {
            configurator.putValue(storedForceImageSize, false)
            configurator.putValue(ConfigKeys.FORCE_IMAGE_SIZE.copy(defaultValue = false), true)
        }
    }

    @Test
    fun `typing a number into an int param stores it`() {
        heightField().performTextReplacement("1080")

        verify { configurator.putValue(storedHeight, 1080) }
    }

    @Test
    fun `typing text that is not a number stores nothing`() {
        heightField().performTextReplacement("tall")

        verify(exactly = 0) { configurator.putValue(any(), any()) }
    }

    @Test
    fun `sensor overlay switch drives the injected overlay manager`() {
        switches()[1].performClick()

        assertTrue(overlayManager.showSensorOverlay.value)
        assertFalse(overlayManager.showSyncOverlay.value)
    }

    @Test
    fun `an overlay turned on elsewhere is shown as on`() {
        overlayManager.setSyncOverlay(true)

        switches()[0].assertIsOn()
        switches()[1].assertIsOff()
    }

    private companion object {
        const val STORED_HEIGHT = 720
    }
}