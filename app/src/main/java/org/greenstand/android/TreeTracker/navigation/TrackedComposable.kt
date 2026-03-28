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
package org.greenstand.android.TreeTracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.koin.androidx.compose.get

inline fun <reified T : Any> NavGraphBuilder.trackedComposable(
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T>(deepLinks = deepLinks) { backStackEntry ->
        val exceptionDataCollector = get<ExceptionDataCollector>()
        val routeName = T::class.simpleName ?: "Unknown"
        LaunchedEffect(routeName) {
            exceptionDataCollector.setScreen(routeName)
        }
        content(backStackEntry)
    }
}
