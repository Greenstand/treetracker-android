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
package org.greenstand.android.TreeTracker.uitest.view

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageButtonUiTest : ComposeUiTest() {
    @Test
    fun `language button shows the default language without a view model`() {
        setContent { Box { LanguageButton() } }

        composeTestRule.onNodeWithText("EN").assertIsDisplayed()
    }

    @Test
    fun `clicking the language button navigates to the language picker`() {
        setContent { Box { LanguageButton() } }

        composeTestRule.onNodeWithText("EN").performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, LanguageRoute()), backStack.toList())
        }
    }
}