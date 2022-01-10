package org.greenstand.android.TreeTracker.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    stringRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // TODO customize button visuals
    Button(
        onClick = onClick,
        modifier = modifier.size(height = 46.dp, width = 120.dp),
        enabled = enabled,
    ) {
        Text(
            text = stringResource(id = stringRes)
        )
    }
}

@Composable
fun BoxScope.ArrowButton(
    isEnabled: Boolean = true,
    colors: ButtonColors = AppButtonColors.ProgressGreen,
    isLeft: Boolean,
    onClick: () -> Unit,
) {
    DepthButton(
        isEnabled = isEnabled,
        colors = colors,
        modifier = Modifier
            .align(Alignment.Center)
            .size(height = 62.dp, width = 62.dp),
        onClick = onClick,
    ) {
        Image(
            modifier = if (isLeft) Modifier.rotate(180f) else Modifier,
            painter = painterResource(id = R.drawable.arrow_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = AppColors.GrayShadow)
        )
    }
}

@Composable
fun BoxScope.LanguageButton() {
    val navController = LocalNavHostController.current
    val languageViewModel: LanguagePickerViewModel =
        viewModel(factory = LocalViewModelFactory.current)
    val language: String =
        languageViewModel.currentLanguage.observeAsState(Language).value.toString()

    DepthButton(
        modifier = Modifier
            .align(Alignment.Center)
            .size(width = 100.dp, 60.dp),
        onClick = {
            navController.navigate(NavRoute.Language.create())
        }
    ) {
        Text(language)
    }
}

@Preview(widthDp = 100, heightDp = 100)
@Composable
fun DepthButtonTogglePreview() {
    var isSelected by remember { mutableStateOf(false) }

    DepthButton(
        onClick = {
            isSelected = !isSelected
        },
        isSelected = isSelected
    ) {
        Text("Toggle", Modifier.align(Alignment.Center))
    }
}

@Preview(widthDp = 100, heightDp = 100)
@Composable
fun DepthButtonPreview() {
    DepthButton(
        onClick = {
        }
    ) {
        Text("Button", Modifier.align(Alignment.Center))
    }
}

@Preview(widthDp = 100, heightDp = 100)
@Composable
fun DepthButtonCirclePreview() {
    DepthButton(
        shape = DepthSurfaceShape.Circle,
        onClick = {
        }
    ) {
        Text("Button", Modifier.align(Alignment.Center))
    }
}

@Composable
        /**
         * Button with toggle down animation. Now enables wrap content functionality.
         * For sample usage see [org.greenstand.android.TreeTracker.userselect.UserButton].
         *
         * @param onClick The callback function for click event.
         * @param modifier The modifier to be applied to the layout.
         * @param contentAlignment The alignment of content inside the button.
         * @param isEnabled Set button enabled state.
         * @param isSelected Set button selected state (if selected, will toggle down).
         * @param colors The colors of the button. See [AppButtonColors], can be customized.
         * @param content The child content of the button.
         */
fun DepthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    isEnabled: Boolean = true,
    isSelected: Boolean? = null,
    depth: Float = 20f,
    colors: ButtonColors = AppButtonColors.Default,
    shape: DepthSurfaceShape = DepthSurfaceShape.Rectangle,
    content: @Composable (BoxScope.() -> Unit),
) {
    val contentColor by colors.contentColor(isEnabled)

    var isPressed by remember { mutableStateOf(false) }
    isSelected?.let { isPressed = isSelected }

    val offsetAnimation: Float by animateFloatAsState(targetValue = if (isPressed) 1f else 0f)

    Box(modifier = modifier) {
        DepthSurface(
            color = contentColor,
            shadowColor = colors.backgroundColor(isEnabled).value,
            isPressed = isPressed,
            depth = depth,
            shape = shape,
            modifier = Modifier
                .matchParentSize() // Match the 'content' size, this enables wrap_content.
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            onClick()
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
        )

        Box(
            modifier = Modifier
                .align(contentAlignment)
                .offset {
                    when (shape) {
                        DepthSurfaceShape.Rectangle -> IntOffset(
                            0,
                            (depth * offsetAnimation).toInt()
                        )
                        DepthSurfaceShape.Circle -> IntOffset(
                            0,
                            (depth * offsetAnimation - depth).toInt()
                        )
                    }

                }
        ) {
            content()
        }
    }
}

enum class DepthSurfaceShape {
    Rectangle,
    Circle
}

@Composable
fun DepthSurface(
    modifier: Modifier,
    isPressed: Boolean,
    color: Color,
    shadowColor: Color,
    depth: Float = 20f,
    shape: DepthSurfaceShape,
) {
    val offsetAnimation: Float by animateFloatAsState(targetValue = if (isPressed) 1f else 0f)

    when (shape) {
        DepthSurfaceShape.Rectangle ->
            DepthSurfaceRectangle(
                modifier = modifier,
                color = color,
                shadowColor = shadowColor,
                offset = offsetAnimation,
                depth = depth,
            )
        DepthSurfaceShape.Circle ->
            DepthSurfaceCircle(
                modifier = modifier,
                color = color,
                shadowColor = shadowColor,
                offset = offsetAnimation,
                depth = depth,
            )
    }
}

@Composable
fun DepthSurfaceRectangle(
    modifier: Modifier,
    color: Color,
    shadowColor: Color,
    offset: Float,
    depth: Float = 20f,
) {
    Canvas(modifier = modifier.fillMaxSize()) {

        val innerSizeDelta = 4
        val gutter = innerSizeDelta / 2f

        val outerSize = size
        val innerSize = Size(
            width = outerSize.width - innerSizeDelta,
            height = outerSize.height - depth
        )
        val cornerRadius = CornerRadius(x = 30f, y = 30f)

        val tempOffset = (offset * depth) - gutter
        val innerHeightOffset = if (tempOffset < gutter) {
            gutter
        } else {
            tempOffset
        }
        drawRoundRect(
            color = shadowColor,
            cornerRadius = cornerRadius,
            topLeft = Offset(x = 0f, y = 0f),
            size = outerSize
        )
        drawRoundRect(
            color = color,
            cornerRadius = cornerRadius,
            topLeft = Offset(x = gutter, y = innerHeightOffset),
            size = innerSize
        )
    }
}

@Composable
fun DepthSurfaceCircle(
    modifier: Modifier,
    color: Color,
    shadowColor: Color,
    offset: Float,
    depth: Float,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gutter = 2
        val outerSize = Size(size.width - depth, size.height - depth)

        // Bottom stretched circle
        drawArc(
            color = shadowColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(x = depth / 2, y = 0f),
            size = outerSize
        )
        drawRect(
            color = shadowColor,
            topLeft = Offset(x = depth / 2, y = outerSize.height / 2),
            size = Size(outerSize.width, depth),
        )
        drawArc(
            color = shadowColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(x = depth / 2, y = depth),
            size = outerSize
        )
        // top circle
        drawCircle(
            color = color,
            radius = outerSize.height / 2 - gutter,
            center = Offset(
                x = size.width / 2,
                y = size.height / 2 + (offset * depth) - (depth / 2)
            ),
        )
    }
}
@Composable
fun OrangeAddButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    DepthButton(
        onClick = onClick,
        shape = DepthSurfaceShape.Circle,
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
    DepthButton(
        modifier = modifier.size(70.dp),
        isEnabled = isEnabled,
        colors = AppButtonColors.ProgressGreen,
        onClick = onClick,
        shape = DepthSurfaceShape.Circle,
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
fun CaptureButton(
    modifier: Modifier = Modifier.size(70.dp),
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    DepthButton(
        modifier = modifier,
        isEnabled = isEnabled,
        colors = AppButtonColors.ProgressGreen,
        onClick =onClick,
        shape = DepthSurfaceShape.Circle,
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