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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TreeTrackerButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isSelected: Boolean? = null,
    depth: Float = 8f,
    colors: ButtonColors = AppButtonColors.Default,
    shape: TreeTrackerButtonShape = TreeTrackerButtonShape.Rectangle,
    contentAlignment: Alignment = Alignment.Center,
    borderBrushOverride: Brush? = null,
    onClick: () -> Unit,
    content: @Composable (BoxScope.() -> Unit),
) {
    val haptic = LocalHapticFeedback.current
    val onClickState = rememberUpdatedState(onClick)
    var isPressed by remember { mutableStateOf(false) }
    isSelected?.let { isPressed = isSelected }
    val verticalOffset: Float by animateFloatAsState(
        targetValue = if (isPressed) depth else 0f,
        animationSpec = tween(durationMillis = 100),
    )

    // Parent box handles clicking and sends size constraints to children
    BoxWithConstraints(
        modifier =
            modifier
                .apply {
                    if (shape == TreeTrackerButtonShape.Circle) {
                        aspectRatio(1f)
                    }
                }.pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            onClickState.value.invoke()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            if (isSelected == null) {
                                isPressed = false
                            }
                        },
                    )
                },
    ) {
        val contentColor by colors.contentColor(isEnabled)
        val shadowColor by colors.backgroundColor(isEnabled)

        var width = maxWidth
        var clipShape = MaterialTheme.shapes.small
        var circleStartOffset = 0.dp
        if (shape == TreeTrackerButtonShape.Circle) {
            clipShape = CircleShape
            width = maxWidth - depth.dp
            circleStartOffset = depth.dp
        }

        // Box 1 stays still and is aligned to the bottom
        Box(
            modifier =
                Modifier
                    .padding(
                        top = depth.dp,
                        start = circleStartOffset,
                    ).height(maxHeight - depth.dp)
                    .width(width)
                    .clip(clipShape)
                    .background(color = shadowColor)
                    .align(Alignment.BottomCenter),
        )
        // Box 2 gets pushed down by an animated top padding value which
        // eventually aligns it directly above box 1
        Box(
            contentAlignment = contentAlignment,
            modifier =
                Modifier
                    .padding(
                        top = verticalOffset.dp,
                        start = circleStartOffset,
                    ).height(maxHeight - depth.dp)
                    .width(width)
                    .clip(clipShape)
                    .background(color = contentColor)
                    .border(
                        width = 1.dp,
                        brush = borderBrushOverride ?: SolidColor(shadowColor),
                        shape = clipShape,
                    ),
        ) {
            content()
        }
    }
}

enum class TreeTrackerButtonShape {
    Rectangle,
    Circle,
}

@Preview("TreeTrackerButton")
@Composable
fun TreeTrackerButtonPreview() {
    TreeTrackerTheme {
        Column(
            modifier =
                Modifier
                    .background(AppColors.Gray)
                    .height(300.dp)
                    .width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TreeTrackerButton(
                modifier =
                    Modifier
                        .height(100.dp)
                        .wrapContentWidth(),
                isEnabled = true,
                onClick = {},
            ) {
                Text("Button 1", Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.height(10.dp))
            TreeTrackerButton(
                modifier =
                    Modifier
                        .height(100.dp)
                        .width(100.dp),
                isEnabled = true,
                shape = TreeTrackerButtonShape.Circle,
                onClick = {},
            ) {
                Text("Button 1", Modifier.align(Alignment.Center))
            }
        }
    }
}
