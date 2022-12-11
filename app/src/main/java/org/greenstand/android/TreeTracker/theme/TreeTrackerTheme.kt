package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun TreeTrackerTheme(content: @Composable () -> Unit) {
    return MaterialTheme(
        colors = Colors,
        typography = Typography(),
        shapes = Shapes(small = RoundedCornerShape(12.dp)),
        content = content
    )
}
