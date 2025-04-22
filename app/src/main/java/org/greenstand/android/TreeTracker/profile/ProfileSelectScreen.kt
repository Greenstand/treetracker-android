package org.greenstand.android.TreeTracker.profile

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.view.AppButtonColors

@Composable
fun ProfileSelectScreen() {
    val navController = LocalNavHostController.current
    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = true,
        isNotificationEnabled = true,
        onNavigateForward = { user ->
            navController.navigate(NavRoute.Profile.create(user.id))
        }
    )
}