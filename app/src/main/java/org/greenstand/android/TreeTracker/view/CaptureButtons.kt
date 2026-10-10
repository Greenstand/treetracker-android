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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R

@Composable
fun OrangeAddButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        colors = AppButtonColors.UploadOrange,
        modifier =
            modifier
                .size(70.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.add),
            contentDescription = "",
            modifier =
                Modifier
                    .size(55.dp)
                    .padding(top = 5.dp),
        )
    }
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean,
) {
    TreeTrackerButton(
        modifier = modifier.size(70.dp),
        isEnabled = isEnabled,
        colors = AppButtonColors.ProgressGreen,
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        depth = 10f,
    ) {
        ImageCaptureCircle(
            modifier =
                Modifier
                    .size(60.dp),
            color = AppColors.Green,
            shadowColor = AppColors.Gray,
        )
    }
}

@Composable
fun ImageCaptureCircle(
    modifier: Modifier,
    color: Color,
    shadowColor: Color,
) {
    Canvas(
        modifier = modifier.background(shape = CircleShape, color = color),
    ) {
        drawCircle(
            color = shadowColor,
            radius = 50f,
            center =
                Offset(
                    x = size.width / 2,
                    y = size.height / 2,
                ),
            style =
                Stroke(
                    width = 5.dp.toPx(),
                ),
        )
        drawCircle(
            color = shadowColor,
            radius = 30f,
            center =
                Offset(
                    x = size.width / 2,
                    y = size.height / 2,
                ),
        )
    }
}