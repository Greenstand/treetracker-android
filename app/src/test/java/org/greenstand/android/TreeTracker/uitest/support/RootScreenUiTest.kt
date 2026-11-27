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
package org.greenstand.android.TreeTracker.uitest.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.After
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

abstract class RootScreenUiTest : ComposeUiTest() {
    @After
    fun stopTestKoin() {
        stopKoin()
    }

    fun startKoinWith(vararg testModules: Module) {
        stopKoin()
        startKoin { modules(testModules.toList()) }
    }

    fun setRootContent(content: @Composable () -> Unit) {
        setContent(inspectionMode = false) {
            CompositionLocalProvider(LocalViewModelFactory provides TreeTrackerViewModelFactory()) {
                content()
            }
        }
    }

    fun languageButtonModule(): Module {
        val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)
        every { languageSwitcher.currentLanguage() } returns Language.ENGLISH
        every { languageSwitcher.observeCurrentLanguage() } returns emptyFlow()
        val languagePickerViewModel = LanguagePickerViewModel(languageSwitcher)
        return module { single { languagePickerViewModel } }
    }
}