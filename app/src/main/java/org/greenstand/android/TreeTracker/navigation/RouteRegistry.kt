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

import androidx.navigation3.runtime.NavKey

object RouteRegistry {
    const val ROUTE_USER_SELECT = "user-select"
    const val ROUTE_WALLET_SELECT = "wallet-select"
    const val ROUTE_ADD_ORG = "add-org"
    const val ROUTE_SESSION_NOTE = "session-note"
    const val ROUTE_TREE_HEIGHT = "tree-height-selection"
    const val ROUTE_TREE_CAPTURE = "capture/{profilePicUrl}"
    const val ROUTE_TREE_IMAGE_REVIEW = "tree-image-review/{photoPath}"

    private const val ALIAS_TREE_CAPTURE = "tree-capture"
    private const val ALIAS_TREE_IMAGE_REVIEW = "image-review"

    private val noArgRoutes: Map<String, NavKey> =
        mapOf(
            ROUTE_USER_SELECT to UserSelectRoute,
            ROUTE_WALLET_SELECT to WalletSelectRoute,
            ROUTE_ADD_ORG to AddOrgRoute,
            ROUTE_SESSION_NOTE to SessionNoteRoute,
            ROUTE_TREE_HEIGHT to TreeHeightScreenRoute,
        )

    private val argRoutePatterns: Set<String> =
        setOf(
            ROUTE_TREE_CAPTURE,
            ROUTE_TREE_IMAGE_REVIEW,
            ALIAS_TREE_CAPTURE,
            ALIAS_TREE_IMAGE_REVIEW,
        )

    private val aliasMap: Map<String, String> =
        mapOf(
            ALIAS_TREE_CAPTURE to ROUTE_TREE_CAPTURE,
            ALIAS_TREE_IMAGE_REVIEW to ROUTE_TREE_IMAGE_REVIEW,
        )

    fun resolveNoArgRoute(routeString: String): NavKey? {
        return noArgRoutes[routeString]
            ?: aliasMap[routeString]?.let { noArgRoutes[it] }
    }

    fun isValidRoute(routeString: String): Boolean =
        routeString in noArgRoutes ||
            routeString in argRoutePatterns ||
            routeString in aliasMap

    fun normalize(routeString: String): String = aliasMap[routeString] ?: routeString
}