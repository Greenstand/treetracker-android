package org.greenstand.android.TreeTracker.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

@Composable
fun Root(viewModelFactory: TreeTrackerViewModelFactory) {
    val navController = rememberNavController()

    CompositionLocalProvider(
        LocalViewModelFactory provides viewModelFactory,
        LocalNavHostController provides navController
    ) {
        Host()
    }
}