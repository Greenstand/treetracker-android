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
package org.greenstand.android.TreeTracker.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.greenstand.android.TreeTracker.view.AppColors

@Composable
fun DebugOverlayHost(
    overlayManager: DebugOverlayManager,
    syncProgressTracker: SyncProgressTracker,
    sensorDiagnosticsTracker: SensorDiagnosticsTracker,
) {
    val showSync by overlayManager.showSyncOverlay.collectAsState()
    val showSensor by overlayManager.showSensorOverlay.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Start/stop sensor tracker based on overlay visibility.
    // DisposableEffect guarantees stop() runs when the composable leaves the tree.
    DisposableEffect(showSensor) {
        if (showSensor) {
            sensorDiagnosticsTracker.start()
        } else {
            sensorDiagnosticsTracker.stop()
        }
        onDispose {
            if (showSensor) {
                sensorDiagnosticsTracker.stop()
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .zIndex(1000f),
    ) {
        if (showSync) {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                SyncOverlay(syncProgressTracker)
            }
        }

        if (showSensor) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                SensorOverlay(sensorDiagnosticsTracker)
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(
                visible = showMenu,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Gray.copy(alpha = 0.95f))
                            .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OverlayToggleItem(
                        label = "Sync",
                        isActive = showSync,
                        onClick = { overlayManager.toggleSyncOverlay() },
                    )
                    OverlayToggleItem(
                        label = "Sensors",
                        isActive = showSensor,
                        onClick = { overlayManager.toggleSensorOverlay() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.DeepGray.copy(alpha = 0.7f))
                        .clickable { showMenu = !showMenu },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "D",
                    color = AppColors.Green,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OverlayToggleItem(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) AppColors.Green else AppColors.MediumGray),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}