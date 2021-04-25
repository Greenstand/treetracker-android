package org.greenstand.android.TreeTracker.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.models.NavRoute

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
    TextButton(
        modifier = Modifier.align(Alignment.Center),
        stringRes = R.string.language,
        onClick = {
            navController.navigate(NavRoute.Language.create())
        }
    )
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
fun DepthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    colors: ButtonColors = AppButtonColors.Default,
    isSelected: Boolean? = null,
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
                .fillMaxSize()
                .pointerInput(true) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            if (isSelected != null) {
                                onClick()
                            }
                            awaitRelease()
                            if (isSelected == null) {
                                isPressed = false
                                onClick()
                            }
                        }
                    )
                }
        )
        Box(
            Modifier
                .align(Alignment.Center)
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
        val cornerRadius = CornerRadius(x = 16f, y = 16f)

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
