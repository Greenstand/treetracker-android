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
package org.greenstand.android.TreeTracker.trees

import org.greenstand.android.TreeTracker.map.MapScreen
import org.greenstand.android.TreeTracker.map.MapViewModel
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.treeedit.TreeDetailScreen
import org.greenstand.android.TreeTracker.treeedit.TreeEditUserSelectScreen
import org.greenstand.android.TreeTracker.treeedit.TreeListScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Trees: the tree list, tree detail and edit screens, and the map.
@OptIn(KoinExperimentalAPI::class)
val treesModule =
    module {
        viewModel { MapViewModel(get()) }

        navigation<MapRoute> { _ -> MapScreen() }
        navigation<TreeEditUserSelectRoute> { _ -> TreeEditUserSelectScreen() }
        navigation<TreeListRoute> { route -> TreeListScreen(userWallet = route.userWallet, userName = route.userName) }
        navigation<TreeDetailRoute> { route -> TreeDetailScreen(treeId = route.treeId) }
    }