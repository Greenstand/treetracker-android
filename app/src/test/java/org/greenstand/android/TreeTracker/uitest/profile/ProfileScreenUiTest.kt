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
package org.greenstand.android.TreeTracker.uitest.profile

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import io.mockk.coVerify
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.ProfileRoute
import org.greenstand.android.TreeTracker.profile.ProfileScreen
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProfileScreenUiTest : UserSelectRootUiTest() {
    @Before
    fun setUp() {
        startKoinWith(repositoriesModule())
        backStack.add(ProfileRoute(planterInfoId = planter.id))
        setRootContent { ProfileScreen(userId = planter.id) }
        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasText(planter.firstName)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `loads and shows the planter`() {
        composeTestRule.onNodeWithText("Doe").assertExists()
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).assertExists()
    }

    @Test
    fun `back pops the profile`() {
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `edit profile switches to edit mode`() {
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).performClick()

        composeTestRule.onNodeWithText(string(R.string.cancel_edit)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.save_changes)).assertExists()
    }

    @Test
    fun `saving stores the edited planter and leaves edit mode`() {
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Ann")

        composeTestRule.onNodeWithText(string(R.string.save_changes)).performScrollTo().performClick()

        coVerify(timeout = WAIT_TIMEOUT_MS) { userRepo.updateUser(match { it.firstName == "Ann" }) }
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.save_changes)).assertDoesNotExist()
    }

    private companion object {
        const val WAIT_TIMEOUT_MS = 5_000L
    }
}