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
package org.greenstand.android.TreeTracker.models

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguageSwitcherTest {
    private val prefs = mockk<Preferences>()
    private val editor = mockk<Preferences.Editor>(relaxed = true)
    private val appLocales = slot<LocaleListCompat>()
    private lateinit var languageSwitcher: LanguageSwitcher

    @Before
    fun setUp() {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setApplicationLocales(capture(appLocales)) } just Runs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        languageSwitcher = LanguageSwitcher(prefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun storedLanguage(value: String?) {
        every { prefs.getString(LanguageSwitcher.LANGUAGE_PREF_KEY, any()) } returns value
    }

    @Test
    fun `fromString maps supported language codes ignoring case`() {
        assertEquals(Language.ENGLISH, Language.fromString("en"))
        assertEquals(Language.SWAHILI, Language.fromString("SW"))
        assertEquals(Language.PORTUGUESE, Language.fromString("Pt"))
    }

    @Test
    fun `fromString returns null for unsupported codes`() {
        assertNull(Language.fromString("fr"))
        assertNull(Language.fromString(""))
        assertNull(Language.fromString("english"))
    }

    @Test
    fun `each language uses its two letter locale`() {
        assertEquals("en", Language.ENGLISH.locale.toLanguageTag())
        assertEquals("sw", Language.SWAHILI.locale.toLanguageTag())
        assertEquals("pt", Language.PORTUGUESE.locale.toLanguageTag())
    }

    @Test
    fun `setLanguage saves the language tag`() {
        languageSwitcher.setLanguage(Language.PORTUGUESE)

        verify { editor.putString(LanguageSwitcher.LANGUAGE_PREF_KEY, "pt") }
        verify { editor.commit() }
    }

    @Test
    fun `setLanguage applies the locale to the app`() {
        languageSwitcher.setLanguage(Language.SWAHILI)

        assertEquals("sw", appLocales.captured.toLanguageTags())
    }

    @Test
    fun `currentLanguage reads the saved language`() {
        storedLanguage("sw")

        assertEquals(Language.SWAHILI, languageSwitcher.currentLanguage())
    }

    @Test
    fun `currentLanguage is null when nothing is saved`() {
        storedLanguage(null)

        assertNull(languageSwitcher.currentLanguage())
    }

    @Test
    fun `currentLanguage is null for an unsupported saved value`() {
        storedLanguage("fr")

        assertNull(languageSwitcher.currentLanguage())
    }

    @Test
    fun `applyCurrentLanguage applies the saved language`() {
        storedLanguage("en")

        languageSwitcher.applyCurrentLanguage()

        assertEquals("en", appLocales.captured.toLanguageTags())
        verify { editor.putString(LanguageSwitcher.LANGUAGE_PREF_KEY, "en") }
    }

    @Test
    fun `applyCurrentLanguage does nothing when no language is saved`() {
        storedLanguage(null)

        languageSwitcher.applyCurrentLanguage()

        verify(exactly = 0) { AppCompatDelegate.setApplicationLocales(any()) }
        verify(exactly = 0) { prefs.edit() }
    }

    @Test
    fun `applyCurrentLanguage does nothing for an unsupported saved value`() {
        storedLanguage("fr")

        languageSwitcher.applyCurrentLanguage()

        verify(exactly = 0) { AppCompatDelegate.setApplicationLocales(any()) }
    }

    @Test
    fun `observeCurrentLanguage emits supported languages and skips the rest`() =
        runTest {
            every { prefs.observeString(LanguageSwitcher.LANGUAGE_PREF_KEY, any()) } returns
                flowOf("pt", null, "fr", "EN")

            val languages = languageSwitcher.observeCurrentLanguage().toList()

            assertEquals(listOf(Language.PORTUGUESE, Language.ENGLISH), languages)
        }
}