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
package org.greenstand.android.TreeTracker.uitest.treeheight

import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.treeheight.TreeHeightScreen
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.treeheight.treeHeightColorTag
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Tests for the [TreeHeightScreen] root: ViewModel wiring and capture flow navigation. */
class TreeHeightScreenUiTest : ComposeUiTest() {
    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private val flowNav = mockk<CaptureFlowNavigationController>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(CaptureFlowScopeManager)
        every { CaptureFlowScopeManager.nav } returns flowNav
        val viewModel = TreeHeightSelectionViewModel(treeCapturer)
        setContent(inspectionMode = false) {
            TreeHeightScreen(viewModel = viewModel)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureFlowScopeManager)
    }

    @Test
    fun `selecting a colour records its index on the tree and enlarges it`() {
        composeTestRule.onNodeWithTag(treeHeightColorTag(1)).performClick()
        composeTestRule.waitForIdle()

        verify { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, "1") }
        composeTestRule.onNodeWithTag(treeHeightColorTag(1)).assertWidthIsEqualTo(350.dp)
    }

    @Test
    fun `forward arrow continues the capture flow after a colour is selected`() {
        composeTestRule.onNodeWithTag(treeHeightColorTag(4)).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify { flowNav.navForward(navigator) }
    }

    @Test
    fun `forward arrow does nothing before a colour is selected`() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { flowNav.navForward(any()) }
    }
}