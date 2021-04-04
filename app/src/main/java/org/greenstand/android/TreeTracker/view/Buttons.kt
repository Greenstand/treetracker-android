package org.greenstand.android.TreeTracker.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    content: @Composable (BoxScope.() -> Unit)
) {
    val depth = 15f
    var isPressed by remember { mutableStateOf(false) }
    val offsetAnimation: Float by animateFloatAsState(targetValue = if (isPressed) 1f else 0f)
    Box(modifier = modifier) {
        DepthSurface(
            color = Color.Gray,
            shadowColor = Color.DarkGray,
            isPressed = isPressed,
            depth = depth,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(true) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            awaitRelease()
                            isPressed = false
                            onClick()
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
    depth: Float = 15f,
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