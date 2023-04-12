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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.get
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@ExperimentalComposeApi
@Composable
fun Host() {

    val navController = LocalNavHostController.current

    TreeTrackerTheme {
        NavHost(navController, startDestination = NavRoute.Splash.route) {
            listOf(
                NavRoute.Splash,
                NavRoute.Language,
                NavRoute.SignupFlow,
                NavRoute.Dashboard,
                NavRoute.Org,
                NavRoute.UserSelect,
                NavRoute.WalletSelect,
                NavRoute.AddWallet,
                NavRoute.AddOrg,
                NavRoute.TreeImageReview,
                NavRoute.Selfie,
                NavRoute.TreeCapture,
                NavRoute.TreeHeightScreen,
                NavRoute.Survey,
                NavRoute.MessagesUserSelect,
                NavRoute.IndividualMessageList,
                NavRoute.Chat,
                NavRoute.Announcement,
                NavRoute.SessionNote,
            ).forEach { addNavRoute(it) }
        }
    }
}

fun NavGraphBuilder.addNavRoute(navRoute: NavRoute) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class], navRoute.contentProvider).apply {
            this.route = navRoute.route
            navRoute.arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            navRoute.deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}
