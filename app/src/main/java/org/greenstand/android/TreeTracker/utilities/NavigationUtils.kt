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
package org.greenstand.android.TreeTracker.utilities

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

private var lastNavigationTime = 0L
private const val NAVIGATION_THROTTLE_MS = 500L

fun NavController.throttledNavigate(
    route: Any,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    val now = System.currentTimeMillis()
    if (now - lastNavigationTime >= NAVIGATION_THROTTLE_MS) {
        lastNavigationTime = now
        navigate(route, builder)
    }
}

fun NavController.throttledPopBackStack(): Boolean {
    val now = System.currentTimeMillis()
    if (now - lastNavigationTime >= NAVIGATION_THROTTLE_MS) {
        lastNavigationTime = now
        return popBackStack()
    }
    return false
}