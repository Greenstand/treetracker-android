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
package org.greenstand.android.TreeTracker.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.view.AppColors
import kotlin.math.roundToInt

@Composable
fun SyncOverlay(syncProgressTracker: SyncProgressTracker) {
    val state by syncProgressTracker.state.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(100f) }

    val completedSteps = state.steps.count { it.status == StepStatus.SUCCESS }
    val totalSteps = state.steps.size
    val overallProgress by animateFloatAsState(
        targetValue = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f,
        label = "overallProgress",
    )

    Box(
        modifier =
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }.padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Gray.copy(alpha = 0.92f))
                .padding(12.dp),
    ) {
        Column(modifier = Modifier.width(240.dp)) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sync Progress",
                    color = AppColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text(
                    text =
                        if (state.isActive) {
                            "$completedSteps/$totalSteps"
                        } else if (state.overallError != null) {
                            "Error"
                        } else {
                            "Done"
                        },
                    color = if (state.overallError != null) AppColors.Red else AppColors.MediumGray,
                    fontSize = 11.sp,
                )
            }

            // Overall progress bar
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = overallProgress,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                color = AppColors.Green,
                backgroundColor = AppColors.DeepGray,
            )

            // Step list (expandable)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    state.steps.forEach { stepState ->
                        SyncStepRow(stepState)
                    }

                    state.overallError?.let { error ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            color = AppColors.Red,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStepRow(stepState: SyncStepState) {
    val statusColor by animateColorAsState(
        targetValue =
            when (stepState.status) {
                StepStatus.PENDING -> AppColors.MediumGray
                StepStatus.RUNNING -> AppColors.SkyBlue
                StepStatus.SUCCESS -> AppColors.Green
                StepStatus.ERROR -> AppColors.Red
            },
        label = "statusColor",
    )

    var showError by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .then(
                    if (stepState.errorMessage != null) {
                        Modifier.clickable { showError = !showError }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Status dot
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Step name
            Text(
                text = stepState.step.displayName,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )

            // Progress for tree steps
            if (stepState.itemsTotal > 0) {
                Text(
                    text = "${stepState.itemsCompleted}/${stepState.itemsTotal}",
                    color = AppColors.MediumGray,
                    fontSize = 10.sp,
                )
            }
        }

        // Sub-progress bar for tree steps
        if (stepState.status == StepStatus.RUNNING && stepState.itemsTotal > 0) {
            val progress by animateFloatAsState(
                targetValue = stepState.itemsCompleted.toFloat() / stepState.itemsTotal,
                label = "stepProgress",
            )
            LinearProgressIndicator(
                progress = progress,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 2.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                color = AppColors.SkyBlue,
                backgroundColor = AppColors.DeepGray,
            )
        }

        // Error message (expandable)
        AnimatedVisibility(visible = showError && stepState.errorMessage != null) {
            Text(
                text = stepState.errorMessage ?: "",
                color = AppColors.Red.copy(alpha = 0.8f),
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}