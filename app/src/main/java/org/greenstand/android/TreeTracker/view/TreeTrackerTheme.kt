package org.greenstand.android.TreeTracker.view

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable

@Composable
fun TreeTrackerTheme(content: @Composable () -> Unit) {
    return MaterialTheme(
        colors = Colors,
        shapes = Shapes(),
        typography = Typography(),
        content = content
    )
}