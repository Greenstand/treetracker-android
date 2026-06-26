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
package org.greenstand.android.TreeTracker.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

class ExceptionDataCollector(
    private val firebaseCrashlytics: FirebaseCrashlytics,
) {
    private var currentRoute: String? = null
    private var lastRoute: String? = null

    fun setScreen(route: String) {
        if (route == currentRoute) {
            return
        }

        if (currentRoute == null) {
            currentRoute = route
        } else {
            lastRoute = currentRoute
            currentRoute = route
        }
        set(CrashKey.ROUTE, route)
        set(CrashKey.LAST_ROUTE, lastRoute)
    }

    fun set(
        key: CrashKey,
        value: String?,
    ) {
        value ?: return

        if (key == CrashKey.USER_WALLET || key == CrashKey.POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId(value)
            firebaseCrashlytics.setCustomKey(key.toString, value)
        } else {
            firebaseCrashlytics.setCustomKey(key.toString, value)
        }
    }

    fun set(
        key: CrashKey,
        value: Boolean,
    ) {
        firebaseCrashlytics.setCustomKey(key.toString, value)
    }

    fun clear(key: CrashKey) {
        if (key == CrashKey.USER_WALLET || key == CrashKey.POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId("")
        }
        firebaseCrashlytics.setCustomKey(key.toString, "")
    }

    private fun String.toCrashKey(): CrashKey {
        return CrashKey.entries.find { it.toString == this } ?: throw IllegalArgumentException("No CrashKey found for key: $this")
    }
}