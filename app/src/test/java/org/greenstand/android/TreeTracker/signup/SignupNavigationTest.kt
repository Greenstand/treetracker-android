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
package org.greenstand.android.TreeTracker.signup

import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Test

class SignupNavigationTest {
    @Test
    fun `first run signup leaves only the Dashboard on the stack`() {
        val navigator = fakeNavigator(LanguageRoute(isFromTopBar = false), SignupFlowRoute)

        navigateToDashboardAfterSignup(navigator)

        assertEquals(listOf(DashboardRoute), navigator.backStack)
    }

    @Test
    fun `signup after logout leaves only the Dashboard on the stack`() {
        // Logout clears the stack down to Signup, so LanguageRoute is not on it.
        val navigator = fakeNavigator(SignupFlowRoute)

        navigateToDashboardAfterSignup(navigator)

        assertEquals(listOf(DashboardRoute), navigator.backStack)
    }

    @Test
    fun `signup with the language picker opened from the top bar leaves only the Dashboard`() {
        val navigator =
            fakeNavigator(
                LanguageRoute(isFromTopBar = false),
                SignupFlowRoute,
                LanguageRoute(isFromTopBar = true),
                SignupFlowRoute,
            )

        navigateToDashboardAfterSignup(navigator)

        assertEquals(listOf(DashboardRoute), navigator.backStack)
    }
}
