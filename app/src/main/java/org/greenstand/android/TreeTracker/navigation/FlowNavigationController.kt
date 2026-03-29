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

import org.greenstand.android.TreeTracker.models.organization.Destination

/**
 * Base class for org-configured flow navigation controllers.
 * Provides common index-tracking, bounds checking, and route resolution logic.
 */
abstract class FlowNavigationController(
    protected val navPath: List<Destination>,
) {
    protected var currentNavPathIndex = 0

    val currentDestination: Destination?
        get() = navPath.getOrNull(currentNavPathIndex)

    val isAtEnd: Boolean
        get() = currentNavPathIndex >= navPath.size - 1

    val isAtStart: Boolean
        get() = currentNavPathIndex <= 0

    protected fun incrementIndex(): Boolean {
        if (currentNavPathIndex < navPath.size) {
            currentNavPathIndex++
            return true
        }
        return false
    }

    protected fun decrementIndex(): Boolean {
        currentNavPathIndex--
        return currentNavPathIndex >= 0
    }

    protected fun resetIndex() {
        currentNavPathIndex = 0
    }

    /**
     * Resolves a Destination to a typed route object for no-arg routes.
     * Returns null for routes that require arguments.
     */
    protected fun resolveNoArgDestination(destination: Destination): Any? {
        val normalized = RouteRegistry.normalize(destination.route)
        return RouteRegistry.resolveNoArgRoute(normalized)
    }
}