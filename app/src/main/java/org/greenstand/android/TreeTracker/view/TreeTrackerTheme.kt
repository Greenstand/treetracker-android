package org.greenstand.android.TreeTracker.view

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Colors = lightColors(
    primary = Color.Gray,
    primaryVariant = Color.DarkGray,
    onPrimary = Color.White,
    secondary = Color.Blue,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color.Red,
    onError = Color.White
)

@Composable
fun TreeTrackerTheme(content: @Composable () -> Unit) {
    return MaterialTheme(
        colors = Colors,
        shapes = Shapes(),
        typography = Typography(),
        content = content
    )
}