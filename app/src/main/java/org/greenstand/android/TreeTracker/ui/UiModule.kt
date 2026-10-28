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
package org.greenstand.android.TreeTracker.ui

import org.greenstand.android.TreeTracker.camera.ImageReviewScreen
import org.greenstand.android.TreeTracker.camera.SelfieScreen
import org.greenstand.android.TreeTracker.camera.SelfieViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.navigation.ImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.SelfieRoute
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Shared screens and ViewModels used by several features (core:ui).
@OptIn(KoinExperimentalAPI::class)
val uiModule =
    module {
        viewModel { LanguagePickerViewModel(get()) }
        viewModel { SelfieViewModel(get()) }
        viewModel { UserSelectViewModel(null, get(), get(), get(), get()) }

        navigation<SelfieRoute> { _ -> SelfieScreen() }
        navigation<ImageReviewRoute> { route -> ImageReviewScreen(route.photoPath) }
    }