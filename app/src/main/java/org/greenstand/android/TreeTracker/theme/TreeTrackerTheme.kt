package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.theme.CustomTextColors
import org.greenstand.android.TreeTracker.theme.CustomTypography

@Composable
fun TreeTrackerTheme(content: @Composable () -> Unit) {
    return MaterialTheme(
        colors = Colors,
        typography = Typography(),
        shapes = Shapes(small = RoundedCornerShape(12.dp)),
        content = content
    )
}



val LocalTreetrackerColors = staticCompositionLocalOf {
    CustomTextColors(
        lightText = Color.Unspecified,
        darkText = Color.Unspecified,
        primaryText = Color.Unspecified,
        uploadText = Color.Unspecified
    )
}
val LocalTreetrackerTypography = staticCompositionLocalOf {
    CustomTypography(
        small = TextStyle.Default,
        regular = TextStyle.Default,
        medium = TextStyle.Default,
        large = TextStyle.Default
    )
}
