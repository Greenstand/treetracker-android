package org.greenstand.android.TreeTracker.messages

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.view.AppButtonColors

@Composable
fun MessagesUserSelectScreen() {
    UserSelect(
        navigationButtonColors = AppButtonColors.MessagePurple,
        isCreateUserEnabled = false,
        isNotificationEnabled = true,
        onNextRoute = { user ->
            TODO()
        }
    )
}
