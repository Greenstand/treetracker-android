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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.splash.Splash
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.viewmodel.LocalSnackbarController
import org.greenstand.android.TreeTracker.viewmodel.SnackbarController
import org.greenstand.android.TreeTracker.viewmodel.resolve
import org.koin.compose.koinInject

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

@ExperimentalComposeApi
@Composable
fun Root(viewModelFactory: TreeTrackerViewModelFactory) {
    val navController = rememberNavController()
    val snackbarController = remember { SnackbarController() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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

    LaunchedEffect(snackbarController) {
        snackbarController.events.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result =
                snackbarHostState.showSnackbar(
                    message = context.resolve(event.message),
                    actionLabel = event.actionLabel?.let { context.resolve(it) },
                    duration = event.duration,
                )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    CompositionLocalProvider(
        LocalViewModelFactory provides viewModelFactory,
        LocalNavHostController provides navController,
        LocalSnackbarController provides snackbarController,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isOrgReady) {
                Host()
            } else {
                Splash()
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .wrapContentHeight(Alignment.Bottom),
            ) { data ->
                Snackbar(
                    contentColor = CustomTheme.textColors.darkText,
                    backgroundColor = AppColors.Green,
                ) {
                    Text(
                        text = data.message,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}