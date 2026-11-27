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
package org.greenstand.android.TreeTracker.uitest.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.recordPressesFor
import org.greenstand.android.TreeTracker.navigation.rememberDoubleTapGuard
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DoubleTapGuardUiTest : ComposeUiTest() {
    private var guardedClicks = 0

    private fun setHarness() {
        setContent {
            val guard = rememberDoubleTapGuard()
            Box(Modifier.fillMaxSize().recordPressesFor(navigator)) {
                Column {
                    Box(Modifier.size(120.dp).testTag(GO).clickable { navigator.navigate(AddOrgRoute) })
                    Box(
                        Modifier
                            .size(120.dp)
                            .then(guard.modifier)
                            .testTag(NEXT)
                            .clickable { if (!guard.isSecondTapOfDoubleTap(navigator)) guardedClicks++ },
                    )
                }
            }
        }
    }

    private fun center(tag: String): Offset =
        composeTestRule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .boundsInRoot.center

    private fun tapGoThenNext(
        holdMs: Long,
        gapMs: Long,
    ) {
        setHarness()
        val go = center(GO)
        val next = center(NEXT)
        composeTestRule.onRoot().performTouchInput {
            down(go)
            advanceEventTime(holdMs)
            up()
            advanceEventTime(gapMs)
            down(next)
            advanceEventTime(40)
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf(DashboardRoute, AddOrgRoute), backStack.toList())
    }

    @Test
    fun `a quick second tap after the tap that navigated is dropped`() {
        tapGoThenNext(holdMs = 40, gapMs = 100)

        assertEquals(0, guardedClicks)
    }

    @Test
    fun `the double-tap timeout runs from the first tap's release, so a held first tap still counts`() {
        tapGoThenNext(holdMs = 150, gapMs = 250)

        assertEquals(0, guardedClicks)
    }

    @Test
    fun `a second tap after the double-tap timeout is meant for the screen`() {
        tapGoThenNext(holdMs = 40, gapMs = 400)

        assertEquals(1, guardedClicks)
    }

    private companion object {
        const val GO = "go"
        const val NEXT = "next"
    }
}