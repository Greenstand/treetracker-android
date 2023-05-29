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

import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager

class TimeProvider(private val locationUpdateManager: LocationUpdateManager) {

    fun currentTime(): Instant {
        val location = locationUpdateManager.currentLocation
        val locationTime = location?.time?.let { Instant.fromEpochMilliseconds(it) }
        return locationTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
    }
}