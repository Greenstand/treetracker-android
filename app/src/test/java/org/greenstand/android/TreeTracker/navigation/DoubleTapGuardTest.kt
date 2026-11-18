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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapGuardTest {
    @Test
    fun `a press within the double-tap timeout of the navigation is the second tap of a double tap`() {
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 0, millisSincePress = 0, doubleTapTimeoutMs = 300))
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 299, millisSincePress = 0, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `a press that reached the app late is judged by when it went down`() {
        // Navigated 700 ms ago; the press went down 500 ms ago, 200 ms after the navigation.
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 700, millisSincePress = 500, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `a press that went down before the navigation counts as the second tap`() {
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 100, millisSincePress = 150, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `a press at or after the double-tap timeout, or with no navigation yet, is meant for the screen`() {
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = 300, millisSincePress = 0, doubleTapTimeoutMs = 300))
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = 1_000, millisSincePress = 100, doubleTapTimeoutMs = 300))
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = Long.MAX_VALUE, millisSincePress = -5, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `a press time in the future counts as now`() {
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = 400, millisSincePress = -50, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `with no press recorded the guard only looks at the navigation`() {
        val clock = FakeClockForGuard()
        val nav = Navigator(mutableListOf(DashboardRoute), clock = clock::now)
        val guard = DoubleTapGuard(uptimeMillis = clock::now, doubleTapTimeoutMs = { 300L })

        assertFalse(guard.isSecondTapOfDoubleTap(nav))
        nav.navigate(UserSelectRoute)
        assertTrue(guard.isSecondTapOfDoubleTap(nav))
        clock.time += 300
        assertFalse(guard.isSecondTapOfDoubleTap(nav))
    }

    @Test
    fun `a recorded press is used up by one check, so a later keyboard action is judged as now`() {
        val clock = FakeClockForGuard()
        val nav = Navigator(mutableListOf(DashboardRoute), clock = clock::now)
        val guard = DoubleTapGuard(uptimeMillis = clock::now, doubleTapTimeoutMs = { 300L })
        nav.navigate(AddOrgRoute)
        // The user taps the org field 50 ms after arriving: too soon, so that tap is dropped.
        clock.time += 50
        guard.recordPress(clock.time)
        assertTrue(guard.isSecondTapOfDoubleTap(nav))

        // Seconds later they press Go on the keyboard, with no new finger down.
        clock.time += 3_000
        assertFalse(guard.isSecondTapOfDoubleTap(nav))
    }

    @Test
    fun `the second tap of a double tap that reached the app late is dropped`() {
        val clock = FakeClockForGuard()
        val nav = Navigator(mutableListOf(UserSelectRoute), clock = clock::now)
        val guard = DoubleTapGuard(uptimeMillis = clock::now, doubleTapTimeoutMs = { 300L })
        nav.navigate(AddOrgRoute)
        val secondTapDown = clock.time + 120
        // The app only handles the click 500 ms after the navigation.
        clock.time += 500
        guard.recordPress(secondTapDown)

        assertTrue(guard.isSecondTapOfDoubleTap(nav))
    }

    private class FakeClockForGuard {
        var time = 1_000L

        fun now(): Long = time
    }
}