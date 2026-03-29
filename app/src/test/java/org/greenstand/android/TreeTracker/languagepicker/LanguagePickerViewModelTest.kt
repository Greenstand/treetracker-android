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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.junit.Assert.assertEquals
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
    fun `Current language returns current language from the language switcher`() = runBlocking {
        val result = testSubject.state.value.currentLanguage
        assertEquals(result, Language.ENGLISH)
    }

    @Test
    fun `Verify set language updates state without applying`() = runBlocking {
        val language = Language.SWAHILI
        testSubject.handleAction(LanguagePickerAction.SetLanguage(language))
        assertEquals(Language.SWAHILI, testSubject.state.value.currentLanguage)
        coVerify(exactly = 0) { languageSwitcher.setLanguage(any()) }
    }

    @Test
    fun `Verify confirm language calls set language from language switcher`() = runBlocking {
        testSubject.handleAction(LanguagePickerAction.SetLanguage(Language.SWAHILI))
        testSubject.handleAction(LanguagePickerAction.ConfirmLanguage)
        coVerify { languageSwitcher.setLanguage(Language.SWAHILI) }
    }
}
