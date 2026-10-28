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

import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationResultTest {
    private val clock = FakeClock()
    private val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)

    @Test
    fun `the screen underneath gets the result once`() {
        assertTrue(navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, 42L))

        assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
        assertEquals(42L, navigator.takeResult(SignupFlowRoute.NewUserId))
        assertNull(navigator.takeResult(SignupFlowRoute.NewUserId))
    }

    @Test
    fun `a different key gets nothing`() {
        navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, 42L)

        assertNull(navigator.takeResult(NavResultKey<Long>("other")))
    }

    @Test
    fun `a result nobody read is dropped when the back stack changes`() {
        navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, 42L)
        clock.advance(300)
        navigator.navigate(WalletSelectRoute)
        navigator.popBackStack()

        assertNull(navigator.takeResult(SignupFlowRoute.NewUserId))
    }

    @Test
    fun `nothing to go back to means no result`() {
        val single = fakeNavigator(SignupFlowRoute)

        assertFalse(single.popBackStackWithResult(SignupFlowRoute.NewUserId, 42L))
        assertEquals(listOf<Any>(SignupFlowRoute), single.backStack)
        assertNull(single.takeResult(SignupFlowRoute.NewUserId))
    }

    @Test
    fun `a screen's scoped navigator sees results set through another view`() {
        navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, 7L)

        val screenNavigator = navigator.scopedTo(UserSelectRoute.toString())
        assertEquals(7L, screenNavigator.takeResult(SignupFlowRoute.NewUserId))
    }
}