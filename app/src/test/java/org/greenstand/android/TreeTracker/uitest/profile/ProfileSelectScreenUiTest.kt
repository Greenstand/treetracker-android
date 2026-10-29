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

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.ProfileRoute
import org.greenstand.android.TreeTracker.navigation.ProfileSelectRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.profile.ProfileSelectScreen
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProfileSelectScreenUiTest : UserSelectRootUiTest() {
    @Before
    fun setUp() {
        startKoinWith(userSelectViewModelModule())
        backStack.add(ProfileSelectRoute)
        setRootContent { ProfileSelectScreen() }
    }

    @Test
    fun `forward without a selected planter stays on profile select`() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, ProfileSelectRoute), backStack.toList())
    }

    @Test
    fun `forward with a selected planter opens that planter's profile`() {
        composeTestRule.onNodeWithText("Jane Doe").performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(DashboardRoute, ProfileSelectRoute, ProfileRoute(planterInfoId = planter.id)),
            backStack.toList(),
        )
    }

    @Test
    fun `add planter opens sign up`() {
        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, ProfileSelectRoute, SignupFlowRoute), backStack.toList())
    }
}