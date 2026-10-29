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
package org.greenstand.android.TreeTracker.uitest.languagepicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerAction
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerState
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelect
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSelectUiTest : ComposeUiTest() {
    private val actions = mutableListOf<LanguagePickerAction>()

    private fun setLanguageSelect(state: LanguagePickerState) {
        setContent {
            LanguageSelect(state = state, onHandleAction = { actions += it })
        }
    }

    private fun createViewModel(currentLanguage: Language?): Pair<LanguagePickerViewModel, LanguageSwitcher> {
        val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)
        every { languageSwitcher.currentLanguage() } returns currentLanguage
        every { languageSwitcher.observeCurrentLanguage() } returns emptyFlow()
        return LanguagePickerViewModel(languageSwitcher) to languageSwitcher
    }

    @Test
    fun `shows every language and the logo`() {
        setLanguageSelect(LanguagePickerState())

        Language.entries.forEach { language ->
            composeTestRule.onNodeWithText(language.toString()).assertIsDisplayed()
        }
        composeTestRule.onNodeWithContentDescription("Treetracker icon").assertIsDisplayed()
    }

    @Test
    fun `clicking a language sends SetLanguage`() {
        setLanguageSelect(LanguagePickerState())

        composeTestRule.onNodeWithText(Language.SWAHILI.toString()).performClick()

        assertEquals(listOf(LanguagePickerAction.SetLanguage(Language.SWAHILI)), actions)
    }

    @Test
    fun `next is disabled while no language is selected`() {
        setLanguageSelect(LanguagePickerState(currentLanguage = null))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `next sends NavigateNext once a language is selected`() {
        setLanguageSelect(LanguagePickerState(currentLanguage = Language.ENGLISH))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf<LanguagePickerAction>(LanguagePickerAction.NavigateNext), actions)
    }

    @Test
    fun `root from onboarding saves the language and opens sign up`() {
        backStack.clear()
        backStack.add(LanguageRoute(isFromTopBar = false))
        val (viewModel, languageSwitcher) = createViewModel(currentLanguage = Language.ENGLISH)
        setContent(inspectionMode = false) {
            LanguageSelectScreen(isFromTopBar = false, viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { languageSwitcher.setLanguage(Language.ENGLISH) }
        assertEquals(listOf(LanguageRoute(isFromTopBar = false), SignupFlowRoute), backStack.toList())
    }

    @Test
    fun `root from top bar saves the language and pops back`() {
        backStack.add(LanguageRoute(isFromTopBar = true))
        val (viewModel, languageSwitcher) = createViewModel(currentLanguage = Language.ENGLISH)
        setContent(inspectionMode = false) {
            LanguageSelectScreen(isFromTopBar = true, viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { languageSwitcher.setLanguage(Language.ENGLISH) }
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `root selecting a language enables next and saves the chosen language`() {
        backStack.clear()
        backStack.add(LanguageRoute(isFromTopBar = false))
        val (viewModel, languageSwitcher) = createViewModel(currentLanguage = null)
        setContent(inspectionMode = false) {
            LanguageSelectScreen(isFromTopBar = false, viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(LanguageRoute(isFromTopBar = false)), backStack.toList())

        composeTestRule.onNodeWithText(Language.PORTUGUESE.toString()).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { languageSwitcher.setLanguage(Language.PORTUGUESE) }
        assertEquals(listOf(LanguageRoute(isFromTopBar = false), SignupFlowRoute), backStack.toList())
    }
}