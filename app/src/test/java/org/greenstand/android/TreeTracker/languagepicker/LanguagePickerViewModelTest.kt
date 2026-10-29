/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.languagepicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LanguagePickerViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)

    private lateinit var testSubject: LanguagePickerViewModel

    @Before
    fun setup() {
        coEvery { languageSwitcher.currentLanguage() } returns Language.ENGLISH
        testSubject = LanguagePickerViewModel(languageSwitcher)
    }

    @Test
    fun `Current language returns current language from the language switcher`() =
        runTest {
            val result = testSubject.state.value.currentLanguage
            assertEquals(result, Language.ENGLISH)
        }

    @Test
    fun `Verify set language updates state without applying`() =
        runTest {
            val language = Language.SWAHILI
            testSubject.handleAction(LanguagePickerAction.SetLanguage(language))
            assertEquals(Language.SWAHILI, testSubject.state.value.currentLanguage)
            coVerify(exactly = 0) { languageSwitcher.setLanguage(any()) }
        }

    @Test
    fun `Verify confirm language calls set language from language switcher`() =
        runTest {
            testSubject.handleAction(LanguagePickerAction.SetLanguage(Language.SWAHILI))
            testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)
            coVerify { languageSwitcher.setLanguage(Language.SWAHILI) }
        }

    @Test
    fun `Verify confirm language does nothing when no language is selected`() =
        runTest {
            // Reset to no language selected
            coEvery { languageSwitcher.currentLanguage() } returns null
            testSubject = LanguagePickerViewModel(languageSwitcher)

            testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)

            coVerify(exactly = 0) { languageSwitcher.setLanguage(any()) }
        }

    @Test
    fun `Verify NavigateNext is a no-op in ViewModel`() =
        runTest {
            testSubject.handleAction(LanguagePickerAction.NavigateNext)

            // State unchanged, no language applied
            assertEquals(Language.ENGLISH, testSubject.state.value.currentLanguage)
            coVerify(exactly = 0) { languageSwitcher.setLanguage(any()) }
        }

    @Test
    fun `WHEN created THEN no events are emitted`() =
        runTest {
            assertTrue(testSubject.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN the stored language changes THEN state follows it`() =
        runTest {
            val languages = MutableSharedFlow<Language>()
            every { languageSwitcher.observeCurrentLanguage() } returns languages
            val viewModel = LanguagePickerViewModel(languageSwitcher)

            languages.emit(Language.PORTUGUESE)

            assertEquals(Language.PORTUGUESE, viewModel.state.value.currentLanguage)
        }

    @Test
    fun `GIVEN a pending selection WHEN the stored language changes THEN the stored language replaces it`() =
        runTest {
            val languages = MutableSharedFlow<Language>()
            every { languageSwitcher.observeCurrentLanguage() } returns languages
            val viewModel = LanguagePickerViewModel(languageSwitcher)
            viewModel.handleAction(LanguagePickerAction.SetLanguage(Language.SWAHILI))

            languages.emit(Language.ENGLISH)

            assertEquals(Language.ENGLISH, viewModel.state.value.currentLanguage)
        }

    @Test
    fun `WHEN each language is selected THEN state reflects it`() =
        runTest {
            Language.entries.forEach { language ->
                testSubject.handleAction(LanguagePickerAction.SetLanguage(language))
                assertEquals(language, testSubject.state.value.currentLanguage)
            }
        }

    @Test
    fun `GIVEN no new selection WHEN confirming THEN the current language is applied`() =
        runTest {
            testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)

            verify(exactly = 1) { languageSwitcher.setLanguage(Language.ENGLISH) }
        }

    @Test
    fun `GIVEN several selections WHEN confirming THEN only the last one is applied`() =
        runTest {
            testSubject.handleAction(LanguagePickerAction.SetLanguage(Language.SWAHILI))
            testSubject.handleAction(LanguagePickerAction.SetLanguage(Language.PORTUGUESE))

            testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)

            verify(exactly = 1) { languageSwitcher.setLanguage(Language.PORTUGUESE) }
            verify(exactly = 0) { languageSwitcher.setLanguage(Language.SWAHILI) }
        }

    @Test
    fun `WHEN confirming THEN state keeps the confirmed language and no events are emitted`() =
        runTest {
            testSubject.handleAction(LanguagePickerAction.SetLanguage(Language.SWAHILI))

            testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)

            assertEquals(Language.SWAHILI, testSubject.state.value.currentLanguage)
            assertTrue(testSubject.events.replayCache.isEmpty())
        }
}