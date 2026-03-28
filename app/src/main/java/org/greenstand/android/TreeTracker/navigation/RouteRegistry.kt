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

/**
 * Maps route identifiers (from org config JSON) to type-safe route objects.
 * Org config stores route patterns as strings in the database. This registry provides
 * the bridge between those strings and the typed navigation routes.
 *
 * Supports both legacy route strings (e.g. "capture/{profilePicUrl}") and
 * stable aliases (e.g. "tree-capture") for backwards compatibility.
 */
object RouteRegistry {

    // Stable route identifiers used in org JSON configs.
    // Legacy route strings with path params are kept for backwards compatibility.
    const val ROUTE_USER_SELECT = "user-select"
    const val ROUTE_WALLET_SELECT = "wallet-select"
    const val ROUTE_ADD_ORG = "add-org"
    const val ROUTE_SESSION_NOTE = "session-note"
    const val ROUTE_TREE_HEIGHT = "tree-height-selection"
    const val ROUTE_TREE_CAPTURE = "capture/{profilePicUrl}"
    const val ROUTE_TREE_IMAGE_REVIEW = "tree-image-review/{photoPath}"

    // Stable aliases for routes (new format for org links)
    private const val ALIAS_TREE_CAPTURE = "tree-capture"
    private const val ALIAS_TREE_IMAGE_REVIEW = "image-review"

    private val noArgRoutes: Map<String, Any> = mapOf(
        ROUTE_USER_SELECT to UserSelectRoute,
        ROUTE_WALLET_SELECT to WalletSelectRoute,
        ROUTE_ADD_ORG to AddOrgRoute,
        ROUTE_SESSION_NOTE to SessionNoteRoute,
        ROUTE_TREE_HEIGHT to TreeHeightScreenRoute,
    )

    // Routes that require arguments at navigation time
    private val argRoutePatterns: Set<String> = setOf(
        ROUTE_TREE_CAPTURE,
        ROUTE_TREE_IMAGE_REVIEW,
        ALIAS_TREE_CAPTURE,
        ALIAS_TREE_IMAGE_REVIEW,
    )

    // Map aliases to their canonical route identifiers
    private val aliasMap: Map<String, String> = mapOf(
        ALIAS_TREE_CAPTURE to ROUTE_TREE_CAPTURE,
        ALIAS_TREE_IMAGE_REVIEW to ROUTE_TREE_IMAGE_REVIEW,
    )

    /**
     * Resolves a route string to a no-arg typed route object.
     * Returns null if the route requires arguments or is unknown.
     */
    fun resolveNoArgRoute(routeString: String): Any? {
        // Try direct match first, then alias resolution
        return noArgRoutes[routeString]
            ?: aliasMap[routeString]?.let { noArgRoutes[it] }
    }

    /**
     * Checks whether a route string is recognized by the registry.
     */
    fun isValidRoute(routeString: String): Boolean {
        return routeString in noArgRoutes
            || routeString in argRoutePatterns
            || routeString in aliasMap
    }

    /**
     * Normalizes a route string, resolving aliases to their canonical form.
     */
    fun normalize(routeString: String): String {
        return aliasMap[routeString] ?: routeString
    }
}
