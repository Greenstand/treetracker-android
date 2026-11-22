/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoredBackStackTest {
    @Test
    fun `setup screens on top are popped when the setup is not open`() {
        val backStack = mutableListOf<NavKey>(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddOrgRoute, SessionNoteRoute)

        backStack.dropSetupScreensWithoutSetup(isSetupOpen = false)

        assertEquals(listOf<NavKey>(DashboardRoute, UserSelectRoute), backStack)
    }

    @Test
    fun `the back stack is kept when the setup is open`() {
        val backStack = mutableListOf<NavKey>(DashboardRoute, UserSelectRoute, AddOrgRoute)

        backStack.dropSetupScreensWithoutSetup(isSetupOpen = true)

        assertEquals(listOf<NavKey>(DashboardRoute, UserSelectRoute, AddOrgRoute), backStack)
    }

    @Test
    fun `a back stack without a setup screen on top is kept`() {
        val backStack = mutableListOf<NavKey>(DashboardRoute, UserSelectRoute, AddOrgRoute, TreeCaptureRoute(profilePicUrl = ""))
        val expected = backStack.toList()

        backStack.dropSetupScreensWithoutSetup(isSetupOpen = false)

        assertEquals(expected, backStack)
    }

    @Test
    fun `the last entry is never popped`() {
        val backStack = mutableListOf<NavKey>(AddOrgRoute)

        backStack.dropSetupScreensWithoutSetup(isSetupOpen = false)

        assertEquals(listOf<NavKey>(AddOrgRoute), backStack)
    }
}
