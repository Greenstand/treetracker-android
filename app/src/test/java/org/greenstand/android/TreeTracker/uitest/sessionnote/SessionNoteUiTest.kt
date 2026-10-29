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
package org.greenstand.android.TreeTracker.uitest.sessionnote

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.sessionnote.SessionNote
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteAction
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

internal const val SESSION_NOTE = "Planted along the river bank"

/** Tests for the stateless [SessionNote] screen. */
class SessionNoteUiTest : ComposeUiTest() {
    private val actions = mutableListOf<SessionNoteAction>()

    private fun render(state: SessionNoteState = SessionNoteState()) {
        setContent {
            SessionNote(
                state = state,
                onHandleAction = { actions += it },
            )
        }
    }

    @Test
    fun `empty note shows the placeholder`() {
        render()

        composeTestRule
            .onNodeWithText(string(R.string.add_note_to_session), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `note from state is shown in the field`() {
        render(state = SessionNoteState(note = SESSION_NOTE))

        composeTestRule.onNodeWithText(SESSION_NOTE).assertExists()
    }

    @Test
    fun `typing updates the note`() {
        render()

        composeTestRule.onNode(hasSetTextAction()).performTextInput(SESSION_NOTE)

        assertEquals(listOf(SessionNoteAction.UpdateNote(SESSION_NOTE)), actions)
    }

    @Test
    fun `forward arrow navigates next even without a note`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf(SessionNoteAction.NavigateNext), actions)
    }

    @Test
    fun `keyboard go action navigates next`() {
        render(state = SessionNoteState(note = SESSION_NOTE))

        composeTestRule.onNode(hasSetTextAction()).performClick()
        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(SessionNoteAction.NavigateNext), actions)
    }

    @Test
    fun `back arrow navigates back`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf(SessionNoteAction.NavigateBack), actions)
    }
}