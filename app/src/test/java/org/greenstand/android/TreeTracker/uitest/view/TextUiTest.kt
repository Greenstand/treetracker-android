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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class TextUiTest : ComposeUiTest() {
    @Test
    fun `top bar title shows the Treetracker logo`() {
        setContent { Box(Modifier.size(200.dp)) { TopBarTitle() } }

        composeTestRule.onNodeWithContentDescription(LOGO_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun `long pressing the logo opens dev options in debug and beta builds`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        setContent { Box(Modifier.size(200.dp)) { TopBarTitle() } }

        composeTestRule.onNodeWithContentDescription(LOGO_DESCRIPTION).performTouchInput { longClick() }

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, DevOptionsRoute), backStack.toList())
        }
    }

    @Test
    fun `tapping the logo does not navigate`() {
        setContent { Box(Modifier.size(200.dp)) { TopBarTitle() } }

        composeTestRule.onNodeWithContentDescription(LOGO_DESCRIPTION).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(DashboardRoute), backStack.toList()) }
    }

    private companion object {
        const val LOGO_DESCRIPTION = "Treetracker icon"
    }
}