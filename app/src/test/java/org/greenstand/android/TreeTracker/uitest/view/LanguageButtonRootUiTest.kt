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
package org.greenstand.android.TreeTracker.uitest.view

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.dsl.module

/** Tests for the top bar [LanguageButton] backed by a real [LanguagePickerViewModel]. */
class LanguageButtonRootUiTest : RootScreenUiTest() {
    private val observedLanguage = MutableSharedFlow<Language>(replay = 1)

    private fun launch(currentLanguage: Language?) {
        val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)
        every { languageSwitcher.currentLanguage() } returns currentLanguage
        every { languageSwitcher.observeCurrentLanguage() } returns observedLanguage
        val viewModel = LanguagePickerViewModel(languageSwitcher)
        startKoinWith(module { single { viewModel } })
        setRootContent { Box { LanguageButton() } }
    }

    @Test
    fun `shows the current language of the app`() {
        launch(currentLanguage = Language.SWAHILI)

        composeTestRule.onNodeWithText(Language.SWAHILI.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText("EN").assertDoesNotExist()
    }

    @Test
    fun `falls back to EN while no language is set`() {
        launch(currentLanguage = null)

        composeTestRule.onNodeWithText("EN").assertIsDisplayed()
    }

    @Test
    fun `follows a language change reported by the switcher`() {
        launch(currentLanguage = Language.ENGLISH)

        observedLanguage.tryEmit(Language.PORTUGUESE)

        composeTestRule.onNodeWithText(Language.PORTUGUESE.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText(Language.ENGLISH.toString()).assertDoesNotExist()
    }

    @Test
    fun `clicking opens the language picker from the top bar`() {
        launch(currentLanguage = Language.ENGLISH)

        composeTestRule.onNodeWithText(Language.ENGLISH.toString()).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(DashboardRoute, LanguageRoute()), backStack.toList()) }
    }
}