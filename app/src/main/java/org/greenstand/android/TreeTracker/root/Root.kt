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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.splash.Splash
import org.koin.compose.koinInject

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

@ExperimentalComposeApi
@Composable
fun Root(viewModelFactory: TreeTrackerViewModelFactory) {
    val navController = rememberNavController()

    // OrgRepo holds its current org only in memory, so it is lost on process death.
    // When the OS restores the app into a deep screen the splash screen — and therefore
    // OrgRepo.init() — is bypassed, and any screen that reads currentOrg() would crash.
    // Re-hydrate it once per process before rendering the (possibly restored) back stack.
    val orgRepo = koinInject<OrgRepo>()
    var isOrgReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        orgRepo.ensureInitialized()
        isOrgReady = true
    }

    CompositionLocalProvider(
        LocalViewModelFactory provides viewModelFactory,
        LocalNavHostController provides navController,
    ) {
        if (isOrgReady) {
            Host()
        } else {
            Splash()
        }
    }
}
