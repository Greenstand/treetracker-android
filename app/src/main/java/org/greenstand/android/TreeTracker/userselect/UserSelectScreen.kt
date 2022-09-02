package org.greenstand.android.TreeTracker.userselect

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.AppButtonColors

@Composable
fun UserSelectScreen() {
    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = true,
        isNotificationEnabled = true,
        onNextRoute = { user ->
            NavRoute.WalletSelect.create()
        }
    )
}
