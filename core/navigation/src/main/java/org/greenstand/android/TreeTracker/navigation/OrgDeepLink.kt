/*
 * Copyright 2026 Treetracker
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

import android.content.Intent

private const val ORG_LINK_SCHEME = "app"
private const val ORG_LINK_HOST = "mobile.treetracker.org"
private const val ORG_LINK_PATH_PREFIX = "/org"

/**
 * Parses the org deep link (`app://mobile.treetracker.org/org?id={orgId}&name={orgName}`)
 * from the launching intent into the start route. Navigation 3 has no built-in deep link
 * support, so this replaces the Nav2 `navDeepLink` on the splash destination. Handled at
 * cold start only, matching the pre-migration behavior.
 */
fun parseStartRoute(intent: Intent?): SplashRoute {
    val uri = intent?.data ?: return SplashRoute()
    val isOrgLink =
        uri.scheme == ORG_LINK_SCHEME &&
            uri.host == ORG_LINK_HOST &&
            uri.path?.startsWith(ORG_LINK_PATH_PREFIX) == true
    if (!isOrgLink) return SplashRoute()
    return SplashRoute(
        orgId = uri.getQueryParameter("id"),
        orgName = uri.getQueryParameter("name"),
    )
}