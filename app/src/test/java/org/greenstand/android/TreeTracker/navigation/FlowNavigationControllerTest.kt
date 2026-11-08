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
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowNavigationControllerTest {
    /** Exposes the protected index helpers of the base class. */
    private class TestFlowController(
        navPath: List<Destination>,
    ) : FlowNavigationController(navPath) {
        val index: Int get() = currentNavPathIndex

        fun increment(): Boolean = incrementIndex()

        fun decrement(): Boolean = decrementIndex()

        fun reset() = resetIndex()

        fun resolve(destination: Destination): NavKey? = resolveNoArgDestination(destination)
    }

    private val threeSteps = destinations("user-select", "wallet-select", "session-note")

    @Test
    fun `starts at the first destination`() {
        val controller = TestFlowController(threeSteps)

        assertEquals(0, controller.index)
        assertEquals(Destination("user-select"), controller.currentDestination)
        assertTrue(controller.isAtStart)
        assertFalse(controller.isAtEnd)
    }

    @Test
    fun `incrementIndex moves to the next destination`() {
        val controller = TestFlowController(threeSteps)

        assertTrue(controller.increment())

        assertEquals(1, controller.index)
        assertEquals(Destination("wallet-select"), controller.currentDestination)
        assertFalse(controller.isAtStart)
        assertFalse(controller.isAtEnd)
    }

    @Test
    fun `isAtEnd is true on the last destination`() {
        val controller = TestFlowController(threeSteps)

        controller.increment()
        controller.increment()

        assertTrue(controller.isAtEnd)
        assertEquals(Destination("session-note"), controller.currentDestination)
    }

    @Test
    fun `incrementIndex can move one past the end and no further`() {
        val controller = TestFlowController(threeSteps)
        repeat(3) { assertTrue(controller.increment()) }

        assertFalse(controller.increment())

        assertEquals(3, controller.index)
        assertNull(controller.currentDestination)
        assertTrue(controller.isAtEnd)
    }

    @Test
    fun `decrementIndex returns true while the index stays in bounds`() {
        val controller = TestFlowController(threeSteps)
        controller.increment()

        assertTrue(controller.decrement())

        assertEquals(0, controller.index)
        assertTrue(controller.isAtStart)
    }

    @Test
    fun `decrementIndex below the first destination returns false`() {
        val controller = TestFlowController(threeSteps)

        assertFalse(controller.decrement())

        assertEquals(-1, controller.index)
        assertNull(controller.currentDestination)
        assertTrue(controller.isAtStart)
    }

    @Test
    fun `resetIndex returns to the first destination`() {
        val controller = TestFlowController(threeSteps)
        controller.increment()
        controller.increment()

        controller.reset()

        assertEquals(0, controller.index)
        assertEquals(Destination("user-select"), controller.currentDestination)
    }

    @Test
    fun `empty flow is both at start and at end`() {
        val controller = TestFlowController(emptyList())

        assertNull(controller.currentDestination)
        assertTrue(controller.isAtStart)
        assertTrue(controller.isAtEnd)
    }

    @Test
    fun `single destination flow is both at start and at end`() {
        val controller = TestFlowController(destinations("user-select"))

        assertTrue(controller.isAtStart)
        assertTrue(controller.isAtEnd)
    }

    @Test
    fun `resolveNoArgDestination maps no-arg routes to their NavKey`() {
        val controller = TestFlowController(threeSteps)

        assertEquals(UserSelectRoute, controller.resolve(Destination("user-select")))
        assertEquals(WalletSelectRoute, controller.resolve(Destination("wallet-select")))
        assertEquals(AddOrgRoute, controller.resolve(Destination("add-org")))
        assertEquals(SessionNoteRoute, controller.resolve(Destination("session-note")))
        assertEquals(TreeHeightScreenRoute, controller.resolve(Destination("tree-height-selection")))
    }

    @Test
    fun `resolveNoArgDestination ignores destination features`() {
        val controller = TestFlowController(threeSteps)

        assertEquals(
            SessionNoteRoute,
            controller.resolve(Destination("session-note", features = listOf("force_note"))),
        )
    }

    @Test
    fun `resolveNoArgDestination returns null for routes that need arguments`() {
        val controller = TestFlowController(threeSteps)

        assertNull(controller.resolve(Destination("capture/{profilePicUrl}")))
        assertNull(controller.resolve(Destination("tree-image-review/{photoPath}")))
        assertNull(controller.resolve(Destination("tree-capture")))
        assertNull(controller.resolve(Destination("image-review")))
    }

    @Test
    fun `resolveNoArgDestination returns null for unknown routes`() {
        val controller = TestFlowController(threeSteps)

        assertNull(controller.resolve(Destination("not-a-route")))
    }
}