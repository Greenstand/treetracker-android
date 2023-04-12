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
package org.greenstand.android.TreeTracker.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

@ExperimentalComposeApi
@Composable
fun Root(viewModelFactory: TreeTrackerViewModelFactory) {
    val navController = rememberNavController()

    CompositionLocalProvider(
        LocalViewModelFactory provides viewModelFactory,
        LocalNavHostController provides navController
    ) {
        Host()
    }
}