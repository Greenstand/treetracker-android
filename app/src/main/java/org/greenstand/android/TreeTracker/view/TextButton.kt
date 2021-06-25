package org.greenstand.android.TreeTracker.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController

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
        modifier = modifier.size(height = 46.dp, width = 110.dp),
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
    DepthButton(

        modifier = Modifier
            .padding(18.dp)
            .size(height = 46.dp, width = 120.dp),
        contentAlignment = Alignment.Center,

        colors = AppButtonColors.Default,
        onClick = {
            navController.navigate(NavRoute.Language.create())
        }


    ) {
            Text(
                text = stringResource(id =  R.string.language),
                color = AppColors.Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,  // TODO: Change font to Montserrat.

            )

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

    colors: ButtonColors = AppButtonColors.Default,
    content: @Composable (BoxScope.() -> Unit)
) {
    val depth = 20f
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
            modifier = Modifier
                .matchParentSize()  // Match the 'content' size, this enables wrap_content.
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
                    IntOffset(0, (depth * offsetAnimation).toInt())
                }
        ) {
            content()
        }
    }
}

@Composable
fun DepthSurface(
    modifier: Modifier,
    isPressed: Boolean,
    color: Color,
    shadowColor: Color,
    depth: Float = 20f,
) {

    val offsetAnimation: Float by animateFloatAsState(targetValue = if (isPressed) 1f else 0f)

    Canvas(modifier = modifier.fillMaxSize()) {

        val innerSizeDelta = 4
        val gutter = innerSizeDelta / 2f

        val outerSize = size
        val innerSize = Size(
            width = outerSize.width - innerSizeDelta,
            height = outerSize.height - depth
        )
        val cornerRadius = CornerRadius(x = 30f, y = 30f)

        val tempOffset = (offsetAnimation * depth) - gutter
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

@Immutable
class DepthButtonColors(
    private val shadowColor: Color,
    private val color: Color,
    private val disabledShadowColor: Color,
    private val disabledColor: Color
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
