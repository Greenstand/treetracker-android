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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoredSetupStepsTest {
    private fun stackOf(vararg keys: NavKey): MutableList<NavKey> = mutableListOf(*keys)

    @Test
    fun `keeps the stack when setup has a user`() {
        val stack = stackOf(DashboardRoute, UserSelectRoute, AddOrgRoute)

        stack.dropSetupStepsWithoutUser(hasSetupUser = true)

        assertEquals(listOf(DashboardRoute, UserSelectRoute, AddOrgRoute), stack)
    }

    @Test
    fun `falls back to user select when Add org has no user`() {
        val stack = stackOf(DashboardRoute, UserSelectRoute, AddOrgRoute)

        stack.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(listOf(DashboardRoute, UserSelectRoute), stack)
    }

    @Test
    fun `drops every setup step on top that needs the user`() {
        val stack = stackOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddWalletRoute)

        stack.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(listOf(DashboardRoute, UserSelectRoute), stack)
    }

    @Test
    fun `falls back from session note to user select`() {
        val stack = stackOf(DashboardRoute, UserSelectRoute, AddOrgRoute, SessionNoteRoute)

        stack.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(listOf(DashboardRoute, UserSelectRoute), stack)
    }

    @Test
    fun `keeps screens that do not need the setup user`() {
        val dashboard = stackOf(DashboardRoute)
        val userSelect = stackOf(DashboardRoute, UserSelectRoute)
        val signup = stackOf(DashboardRoute, UserSelectRoute, SignupFlowRoute)

        dashboard.dropSetupStepsWithoutUser(hasSetupUser = false)
        userSelect.dropSetupStepsWithoutUser(hasSetupUser = false)
        signup.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(listOf(DashboardRoute), dashboard)
        assertEquals(listOf(DashboardRoute, UserSelectRoute), userSelect)
        assertEquals(listOf(DashboardRoute, UserSelectRoute, SignupFlowRoute), signup)
    }

    @Test
    fun `keeps setup steps under a capture screen`() {
        val stack = stackOf(DashboardRoute, UserSelectRoute, AddOrgRoute, TreeCaptureRoute(profilePicUrl = ""))

        stack.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(
            listOf(DashboardRoute, UserSelectRoute, AddOrgRoute, TreeCaptureRoute(profilePicUrl = "")),
            stack,
        )
    }

    @Test
    fun `never empties the stack`() {
        val stack = stackOf(AddOrgRoute)

        stack.dropSetupStepsWithoutUser(hasSetupUser = false)

        assertEquals(listOf<NavKey>(AddOrgRoute), stack)
    }

    @Test
    fun `with no scope open the default check finds no user`() {
        CaptureSetupScopeManager.close()
        val stack = stackOf(DashboardRoute, UserSelectRoute, AddOrgRoute)

        stack.dropSetupStepsWithoutUser()

        assertEquals(listOf(DashboardRoute, UserSelectRoute), stack)
    }
}