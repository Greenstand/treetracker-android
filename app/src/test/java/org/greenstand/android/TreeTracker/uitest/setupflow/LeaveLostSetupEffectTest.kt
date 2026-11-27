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
package org.greenstand.android.TreeTracker.uitest.setupflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.models.setupflow.LeaveLostSetupEffect
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LeaveLostSetupEffectTest : ComposeUiTest() {
    private fun resetBackStack(vararg routes: NavKey) {
        backStack.clear()
        backStack.addAll(routes)
    }

    @Test
    fun `does nothing while the setup is not lost`() {
        resetBackStack(DashboardRoute, UserSelectRoute, WalletSelectRoute)

        setContent { LeaveLostSetupEffect(setupLost = false) }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), backStack.toList())
    }

    @Test
    fun `pops back to the dashboard when the setup is lost`() {
        resetBackStack(DashboardRoute, UserSelectRoute, WalletSelectRoute, SessionNoteRoute)

        setContent { LeaveLostSetupEffect(setupLost = true) }
        composeTestRule.waitForIdle()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }

    @Test
    fun `opens the dashboard as the only entry when the restored back stack has none`() {
        resetBackStack(UserSelectRoute, WalletSelectRoute)

        setContent { LeaveLostSetupEffect(setupLost = true) }
        composeTestRule.waitForIdle()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }

    @Test
    fun `leaves once the setup becomes lost after the screen is shown`() {
        resetBackStack(DashboardRoute, UserSelectRoute, WalletSelectRoute)
        var setupLost by mutableStateOf(false)

        setContent { LeaveLostSetupEffect(setupLost = setupLost) }
        composeTestRule.waitForIdle()
        assertEquals(3, backStack.size)

        setupLost = true
        composeTestRule.waitForIdle()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }
}