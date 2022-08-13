package org.greenstand.android.TreeTracker.userselect

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.AppButtonColors

@Composable
fun UserSelectScreen() {
    val navController = LocalNavHostController.current
    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = true,
        isNotificationEnabled = true,
        onNavigateForward = {
            CaptureSetupScopeManager.nav.navForward(navController)
        }
    )
}
