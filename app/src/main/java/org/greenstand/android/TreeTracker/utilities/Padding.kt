package org.greenstand.android.TreeTracker.utilities

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple padding values.
 *
 * Useful for supplying padding values
 * without having to use a Modifier.
 */
data class Padding(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp

)
