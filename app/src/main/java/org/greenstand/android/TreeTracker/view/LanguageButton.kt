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
package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.throttledNavigate

@Composable
fun BoxScope.LanguageButton() {
    val navController = LocalNavHostController.current
    // Avoid creating a viewmodel if we're in Preview Mode.
    // Viewmodels are large, we do not want to load them in the preview
    val languageViewModel: LanguagePickerViewModel? =
        if (!LocalInspectionMode.current) {
            viewModel(factory = LocalViewModelFactory.current)
        } else {
            null
        }
    val languageState = languageViewModel?.state?.collectAsState()
    val language: String = languageState?.value?.currentLanguage?.toString() ?: "EN"

    TreeTrackerButton(
        colors = AppButtonColors.ProgressGreen,
        modifier =
            Modifier
                .align(Alignment.Center)
                .size(width = 100.dp, 60.dp),
        onClick = {
            navController.throttledNavigate(LanguageRoute())
        },
    ) {
        Text(
            text = language,
            fontWeight = FontWeight.Bold,
            color = CustomTheme.textColors.darkText,
            style = CustomTheme.typography.regular,
        )
    }
}