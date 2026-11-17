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
package org.greenstand.android.TreeTracker.userselect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapGuardTest {
    @Test
    fun `a tap inside the double-tap timeout after a navigation is the second tap of a double tap`() {
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 0, doubleTapTimeoutMs = 300))
        assertTrue(isSecondTapOfDoubleTap(millisSinceLastNavigation = 299, doubleTapTimeoutMs = 300))
    }

    @Test
    fun `a tap at or after the double-tap timeout, or with no navigation yet, is meant for the screen`() {
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = 300, doubleTapTimeoutMs = 300))
        assertFalse(isSecondTapOfDoubleTap(millisSinceLastNavigation = Long.MAX_VALUE, doubleTapTimeoutMs = 300))
    }
}