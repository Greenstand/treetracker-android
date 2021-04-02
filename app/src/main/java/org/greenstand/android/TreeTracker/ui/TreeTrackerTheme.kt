package org.greenstand.android.TreeTracker.view

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.ui.*

val Colors = darkColors(
    primary = colorPrimary,
    secondary = accentOrange,
    surface = colorPrimaryDark,
    onSurface = colorPrimaryDark

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
