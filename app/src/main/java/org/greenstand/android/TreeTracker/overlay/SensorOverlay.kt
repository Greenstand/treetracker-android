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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.view.AppColors
import kotlin.math.roundToInt

@Composable
fun SensorOverlay(sensorDiagnosticsTracker: SensorDiagnosticsTracker) {
    val state by sensorDiagnosticsTracker.state.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(100f) }

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
        Column(modifier = Modifier.width(220.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sensors",
                    color = AppColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                StatusDot(
                    color = if (state.gps.isUpdating) AppColors.Green else AppColors.Red,
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    GpsSection(state.gps)
                    Spacer(modifier = Modifier.height(8.dp))
                    StepSection(state.steps)
                }
            }
        }
    }
}

@Composable
private fun GpsSection(gps: GpsState) {
    SectionHeader("GPS")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ConvergenceRing(
            percent = gps.convergencePercent,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            gps.convergenceStatus?.let { status ->
                val (statusText, statusColor) =
                    when (status) {
                        ConvergenceStatus.CONVERGED -> "CONVERGED" to AppColors.Green
                        ConvergenceStatus.NOT_CONVERGED -> "CONVERGING" to AppColors.Yellow
                        ConvergenceStatus.TIMED_OUT -> "TIMED OUT" to AppColors.Red
                    }
                Text(text = statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (gps.latitude != null && gps.longitude != null) {
                Text(
                    text = "%.6f, %.6f".format(gps.latitude, gps.longitude),
                    color = Color.White,
                    fontSize = 10.sp,
                )
                gps.accuracy?.let {
                    Text(text = "Accuracy: %.1fm".format(it), color = AppColors.MediumGray, fontSize = 9.sp)
                }
            } else {
                Text(text = "No GPS fix", color = AppColors.MediumGray, fontSize = 10.sp)
            }
        }
    }

    if (gps.latStdDev != null || gps.lonStdDev != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StdDevLabel("Lat", gps.latStdDev, gps.latThreshold)
            StdDevLabel("Lon", gps.lonStdDev, gps.lonThreshold)
        }
    }
}

@Composable
private fun StdDevLabel(
    label: String,
    stdDev: Double?,
    threshold: Double,
) {
    val color = if (stdDev != null && stdDev < threshold) AppColors.Green else AppColors.Red
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", color = AppColors.MediumGray, fontSize = 9.sp)
        Text(
            text = if (stdDev != null) "%.7f".format(stdDev) else "--",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ConvergenceRing(
    percent: Float,
    modifier: Modifier = Modifier,
) {
    val animatedPercent by animateFloatAsState(targetValue = percent, label = "convergencePercent")
    val ringColor =
        when {
            animatedPercent >= 0.67f -> AppColors.Green
            animatedPercent >= 0.33f -> AppColors.Yellow
            else -> AppColors.Red
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val strokeWidth = 4.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = AppColors.DeepGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedPercent,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${(animatedPercent * 100).toInt()}%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StepSection(steps: StepState) {
    SectionHeader("Steps")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (steps.listenerActive) {
            Text(text = "${steps.deltaSteps}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "steps", color = AppColors.MediumGray, fontSize = 10.sp)
        } else {
            Text(
                text = if (steps.sensorAvailable) "idle" else "no sensor",
                color = AppColors.MediumGray,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        StatusDot(
            color =
                when {
                    steps.listenerActive -> AppColors.Green
                    steps.sensorAvailable -> AppColors.Yellow
                    else -> AppColors.Red
                },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = AppColors.SkyBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
    )
}