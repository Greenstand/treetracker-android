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
package org.greenstand.android.TreeTracker.devtools

import org.greenstand.android.TreeTracker.devoptions.DevOptionsRoot
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Developer tools: the developer options screen (debug builds).
@OptIn(KoinExperimentalAPI::class)
val devToolsModule =
    module {
        viewModel { DevOptionsViewModel(get(), get()) }

        navigation<DevOptionsRoute> { _ -> DevOptionsRoot() }
    }