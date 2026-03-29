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
package org.greenstand.android.TreeTracker.permissions

import android.location.LocationManager
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class PermissionItemsState(
    val isLocationEnabled: Boolean? = null
)

sealed class PermissionAction : Action {
    object CheckLocationEnabled : PermissionAction()
}

class PermissionViewModel(
    private val locationManager: LocationManager
) : BaseViewModel<PermissionItemsState, PermissionAction>(PermissionItemsState()) {

    override fun handleAction(action: PermissionAction) {
        when (action) {
            is PermissionAction.CheckLocationEnabled -> {
                updateState {
                    copy(isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                }
            }
        }
    }
}
