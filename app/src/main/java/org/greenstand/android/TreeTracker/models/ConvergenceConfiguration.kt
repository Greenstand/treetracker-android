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
package org.greenstand.android.TreeTracker.models

import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator

class ConvergenceConfiguration(
    private val configurator: Configurator,
) {

    var locationDataConfig: LocationDataConfig
        private set

    init {
        locationDataConfig = LocationDataConfig()
        refreshConfig()
    }

    fun refreshConfig() {
        locationDataConfig = LocationDataConfig(
            convergenceTimeout = configurator.getInt(ConfigKeys.CONVERGENCE_TIMEOUT).toLong(),
            convergenceDataSize = configurator.getInt(ConfigKeys.CONVERGENCE_DATA_SIZE),
            lonStdDevThreshold = configurator.getFloat(ConfigKeys.LON_STD_DEV_THRESHOLD),
            latStdDevThreshold = configurator.getFloat(ConfigKeys.LAT_STD_DEV_THRESHOLD),
        )
    }
}

data class LocationDataConfig(
    val minTimeBetweenUpdates: Long = 1000L,
    val minDistanceBetweenUpdates: Float = 0F,
    val convergenceTimeout: Long = 20000L,
    val convergenceDataSize: Int = 5,
    val lonStdDevThreshold: Float = 0.00001F,
    val latStdDevThreshold: Float = 0.00001F
)