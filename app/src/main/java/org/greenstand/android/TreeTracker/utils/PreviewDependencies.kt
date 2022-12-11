package org.greenstand.android.TreeTracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@Composable
fun PreviewDependencies(content: @Composable () -> Unit) {
    val navController = rememberNavController()

    CompositionLocalProvider(
        LocalNavHostController provides navController
    ) {
        CustomTheme {
            TreeTrackerTheme {
                content()
            }
        }
    }
}