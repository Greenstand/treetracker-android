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
package org.greenstand.android.TreeTracker.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun NoGPSDeviceDialog(onPositiveClick: () -> Unit){
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.error_outline),
        title = stringResource(R.string.no_gps_device_header),
        textContent = stringResource(R.string.no_gps_device_content),
        onPositiveClick = onPositiveClick
    )
}