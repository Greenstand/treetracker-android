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
package org.greenstand.android.TreeTracker.uitest.capture

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.capture.TreeImageReviewScreen
import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.organization.FeatureResolver
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.greenstand.android.TreeTracker.uitest.TEXT_FIELD_DIALOG_QUALIFIERS
import org.robolectric.annotation.Config

/** Tests for the [TreeImageReviewScreen] root: ViewModel wiring and capture flow navigation. */
@Config(qualifiers = TEXT_FIELD_DIALOG_QUALIFIERS)
class TreeImageReviewScreenUiTest : ComposeUiTest() {
    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val featureResolver = mockk<FeatureResolver>(relaxed = true)
    private val flowNav = mockk<CaptureFlowNavigationController>(relaxed = true)

    private val approveOutsideDialog = hasTestTag(APPROVE_BUTTON_TAG) and !hasAnyAncestor(isDialog())
    private val approveInDialog = hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())

    @Before
    fun setUp() {
        mockkObject(CaptureFlowScopeManager)
        every { CaptureFlowScopeManager.nav } returns flowNav
        every { treeCapturer.currentTree } returns null
        coEvery { userRepo.getPowerUser() } returns powerUser(numberOfTrees = 3)
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureFlowScopeManager)
    }

    private fun render(forceNote: Boolean = false) {
        every { featureResolver.isCaptureFlowFeatureEnabled(any(), any()) } returns forceNote
        val viewModel = TreeImageReviewViewModel(treeCapturer, userRepo, featureResolver)
        setContent(inspectionMode = false) {
            TreeImageReviewScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `approving continues the capture flow`() {
        render()

        composeTestRule.onNode(approveOutsideDialog).performClick()
        composeTestRule.waitForIdle()

        coVerify { flowNav.navForward(navigator) }
    }

    @Test
    fun `approving without a required note opens the note dialog instead`() {
        render(forceNote = true)

        composeTestRule.onNode(approveOutsideDialog).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.add_note_to_tree)).assertExists()
        coVerify(exactly = 0) { flowNav.navForward(any()) }
    }

    @Test
    fun `declining steps back in the capture flow`() {
        render()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { flowNav.navBackward(navigator) }
    }

    @Test
    fun `adding a note saves it on the tree and closes the dialog`() {
        render()

        composeTestRule.onNodeWithText(string(R.string.note)).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(TREE_NOTE)
        composeTestRule.onNode(approveInDialog).performClick()
        composeTestRule.waitForIdle()

        verify { treeCapturer.setNote(TREE_NOTE) }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `review tutorial is shown on a planter's first tree`() {
        coEvery { userRepo.getPowerUser() } returns powerUser(numberOfTrees = 0)
        render()

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertExists()
    }

    @Test
    fun `review tutorial is not shown to an experienced planter`() {
        render()

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertDoesNotExist()
    }

    private fun powerUser(numberOfTrees: Int): User =
        User(
            id = 7,
            wallet = "fay-wallet",
            numberOfTrees = numberOfTrees,
            firstName = "Fay",
            lastName = "Achieng",
            photoPath = "/no/such/fay.jpg",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    private companion object {
        const val TREE_NOTE = "Young mango tree"
    }
}