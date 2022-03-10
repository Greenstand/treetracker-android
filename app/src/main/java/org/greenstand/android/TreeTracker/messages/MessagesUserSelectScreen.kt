package org.greenstand.android.TreeTracker.messages

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors

@Composable
fun MessagesUserSelectScreen() {
    UserSelect(
        navigationButtonColors = AppButtonColors.MessagePurple,
        isCreateUserEnabled = false,
        isNotificationEnabled = true,
        selectedColor = AppColors.Purple,
        onNextRoute = { user ->
            NavRoute.IndividualMessageList.create(user.id)
        }
    )
}
