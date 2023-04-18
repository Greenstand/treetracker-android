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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme

@Composable
fun BoxScope.ArrowButton(
    isEnabled: Boolean = true,
    colors: ButtonColors = AppButtonColors.ProgressGreen,
    isLeft: Boolean,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        isEnabled = isEnabled,
        colors = colors,
        modifier = Modifier
            .align(Alignment.Center)
            .size(height = 62.dp, width = 62.dp),
        onClick = onClick,
    ) {
        Image(
            modifier = Modifier
                .size(height = 45.dp, width = 45.dp),
            painter = getPainter(colors = colors, isLeft = isLeft),
            contentDescription = null,
        )
    }
}

@Composable
private fun getPainter(colors: ButtonColors, isLeft: Boolean): Painter {
    val painter: Painter
    return when (colors) {
        AppButtonColors.MessagePurple -> {
            painter =
                if (isLeft) painterResource(id = R.drawable.arrow_backward_pink) else painterResource(
                    id = R.drawable.arrow_forward_pink
                )
            painter
        }
        else -> {
            painter =
                if (isLeft) painterResource(id = R.drawable.arrow_left_green) else painterResource(
                    id = R.drawable.arrow_right_green
                )
            painter
        }
    }
}

/**
 * @param onClick The callback function for click event.
 * @param modifier The modifier to be applied to the layout.
 * @param approval Set the type of button to display(if approval is true, shows green thumps up button )
 */
@Composable
fun ApprovalButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    approval: Boolean,
) {
    val color = if (approval) AppButtonColors.ProgressGreen else AppButtonColors.DeclineRed
    val image =
        if (approval) painterResource(id = R.drawable.thumbs_up_green) else painterResource(id = R.drawable.thumbs_down_red)
    TreeTrackerButton(
        colors = color,
        modifier = modifier
            .size(height = 60.dp, width = 60.dp),
        onClick = onClick,
    ) {
        Image(
            painter = image,
            contentDescription = null,
        )
    }
}

@Composable
fun InfoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit, shape: TreeTrackerButtonShape = TreeTrackerButtonShape.Circle

) {
    TreeTrackerButton(
        modifier = modifier.size(60.dp),
        colors = AppButtonColors.WhiteLight,
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        depth = 6f
    )
    {
        Image(
            painter = painterResource(id = R.drawable.info_icon), modifier = Modifier
                .size(80.dp), contentDescription = null
        )
    }

}

@Composable
fun BoxScope.LanguageButton() {
    val navController = LocalNavHostController.current
    // Avoid creating a viewmodel if we're in Preview Mode.
    // Viewmodels are large, we do not want to load them in the preview
    val languageViewModel: LanguagePickerViewModel? = if (!LocalInspectionMode.current) {
        viewModel(factory = LocalViewModelFactory.current)
    } else {
        null
    }
    val language: String =
        languageViewModel?.currentLanguage?.observeAsState(Language)?.value.toString()

    TreeTrackerButton(
        colors = AppButtonColors.ProgressGreen,
        modifier = Modifier
            .align(Alignment.Center)
            .size(width = 100.dp, 60.dp),
        onClick = {
            navController.navigate(NavRoute.Language.create())
        }
    ) {
        Text(
            text = language,
            fontWeight = FontWeight.Bold,
            color = CustomTheme.textColors.darkText,
            style = CustomTheme.typography.regular
        )
    }
}

@Composable
fun BoxScope.UserImageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imagePath: String,
) {
    TreeTrackerButton(
        modifier = modifier
            .align(Alignment.Center)
            .width(100.dp)
            .height(100.dp)
            .padding(
                start = 15.dp,
                top = 10.dp,
                end = 10.dp,
                bottom = 10.dp
            )
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(10.dp)),
        onClick = onClick,
    ) {
        LocalImage(
            modifier = Modifier
                .padding(bottom = 12.dp, end = 1.dp)
                .fillMaxSize()
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(10.dp)),
            imagePath = imagePath,
            contentScale = ContentScale.Crop
        )
    }
}

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
        animationSpec = tween(durationMillis = 100)
    )

    // Parent box handles clicking and sends size constraints to children
    BoxWithConstraints(
        modifier = modifier
            .apply {
                if (shape == TreeTrackerButtonShape.Circle) {
                    aspectRatio(1f)
                }
            }
            .pointerInput(isEnabled) {
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
                    }
                )
            }
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
            modifier = Modifier
                .padding(
                    top = depth.dp,
                    start = circleStartOffset
                )
                .height(maxHeight - depth.dp)
                .width(width)
                .clip(clipShape)
                .background(color = shadowColor)
                .align(Alignment.BottomCenter)
        )
        // Box 2 gets pushed down by an animated top padding value which
        // eventually aligns it directly above box 1
        Box(
            contentAlignment = contentAlignment,
            modifier = Modifier
                .padding(
                    top = verticalOffset.dp,
                    start = circleStartOffset
                )
                .height(maxHeight - depth.dp)
                .width(width)
                .clip(clipShape)
                .background(color = contentColor)
                .border(
                    width = 1.dp,
                    brush = borderBrushOverride ?: SolidColor(shadowColor),
                    shape = clipShape,
                )
        ) {
            content()
        }
    }
}

enum class TreeTrackerButtonShape {
    Rectangle,
    Circle
}

@Composable
fun OrangeAddButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        colors = AppButtonColors.UploadOrange,
        modifier = modifier
            .size(70.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.add),
            contentDescription = "",
            modifier = Modifier
                .size(55.dp)
                .padding(top = 5.dp)
        )
    }
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    TreeTrackerButton(
        modifier = modifier.size(70.dp),
        isEnabled = isEnabled,
        colors = AppButtonColors.ProgressGreen,
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        depth = 10f
    ) {
        ImageCaptureCircle(
            modifier = Modifier
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
        modifier = modifier.background(shape = CircleShape, color = color)

    ) {
        drawCircle(
            color = shadowColor,
            radius = 50f,
            center = Offset(
                x = size.width / 2,
                y = size.height / 2
            ),
            style = Stroke(
                width = 5.dp.toPx()
            )
        )
        drawCircle(
            color = shadowColor,
            radius = 30f,
            center = Offset(
                x = size.width / 2,
                y = size.height / 2
            ),
        )
    }
}

@Immutable
class DepthButtonColors(
    val shadowColor: Color,
    val color: Color,
    val disabledShadowColor: Color,
    val disabledColor: Color
) : ButtonColors {

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) shadowColor else disabledShadowColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) color else disabledColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DepthButtonColors

        if (shadowColor != other.shadowColor) return false
        if (color != other.color) return false
        if (disabledShadowColor != other.disabledShadowColor) return false
        if (disabledColor != other.disabledColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shadowColor.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + disabledShadowColor.hashCode()
        result = 31 * result + disabledColor.hashCode()
        return result
    }
}

@Preview("TreeTrackerButton")
@Composable
fun TreeTrackerButtonPreview() {
    TreeTrackerTheme {
        Column(
            modifier = Modifier
                .background(AppColors.Gray)
                .height(300.dp)
                .width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TreeTrackerButton(
                modifier = Modifier
                    .height(100.dp)
                    .wrapContentWidth(),
                isEnabled = true,
                onClick = {}
            ) {
                Text("Button 1", Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.height(10.dp))
            TreeTrackerButton(
                modifier = Modifier
                    .height(100.dp)
                    .width(100.dp),
                isEnabled = true,
                shape = TreeTrackerButtonShape.Circle,
                onClick = {}
            ) {
                Text("Button 1", Modifier.align(Alignment.Center))
            }
        }
    }
}