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
 * Maps legacy string route patterns (from org config JSON) to type-safe route objects.
 * Org config stores route patterns as strings in the database. This registry provides
 * the bridge between those strings and the typed navigation routes.
 */
object RouteRegistry {

    // Legacy route strings matching what's stored in org JSON configs
    const val ROUTE_USER_SELECT = "user-select"
    const val ROUTE_WALLET_SELECT = "wallet-select"
    const val ROUTE_ADD_ORG = "add-org"
    const val ROUTE_SESSION_NOTE = "session-note"
    const val ROUTE_TREE_HEIGHT = "tree-height-selection"
    const val ROUTE_TREE_CAPTURE = "capture/{profilePicUrl}"
    const val ROUTE_TREE_IMAGE_REVIEW = "tree-image-review/{photoPath}"

    fun resolveNoArgRoute(routeString: String): Any? = when (routeString) {
        ROUTE_USER_SELECT -> UserSelectRoute
        ROUTE_WALLET_SELECT -> WalletSelectRoute
        ROUTE_ADD_ORG -> AddOrgRoute
        ROUTE_SESSION_NOTE -> SessionNoteRoute
        ROUTE_TREE_HEIGHT -> TreeHeightScreenRoute
        else -> null
    }
}
