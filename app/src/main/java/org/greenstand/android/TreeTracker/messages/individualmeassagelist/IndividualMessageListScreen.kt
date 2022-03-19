package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.UserImageButton
import timber.log.Timber

@ExperimentalFoundationApi
@Composable
fun IndividualMessageListScreen(
    userId: Long,
    viewModel: IndividualMessageListViewModel = viewModel(factory = IndividualMessageListViewModelFactory(userId))
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(IndividualMessageListState())

    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                    state.currentUser?.photoPath?.let {
                        UserImageButton(
                            onClick = {
                                navController.popBackStack()
                            },
                            imagePath = it
                        )
                    }
                }
            )
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedMessage != null,
                        colors = AppButtonColors.MessagePurple,
                        onClick = {
                            when(val msg = state.selectedMessage) {
                               is DirectMessage -> navController.navigate(NavRoute.Chat.create(userId, msg.from))
                               is SurveyMessage -> navController.navigate(NavRoute.Survey.create(msg.id))
                               is AnnouncementMessage -> navController.navigate(NavRoute.Announcement.create( msg.id))
                            }
                        }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = AppButtonColors.MessagePurple,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                }
            )
        }
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            modifier = Modifier.padding(it), // Padding for bottom bar.
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp)
        ) {
            items(state.messages) { message ->

                val isSelected = state.selectedMessage == message
                key(message.id) {
                    when (message) {
                        is DirectMessage ->
                            IndividualMessageItem(
                                isSelected = isSelected,
                                isNotificationEnabled = !message.isRead,
                                text = message.from,
                                icon = R.drawable.individual_message_icon,
                                messageTypeText = stringResource(R.string.message)
                            ) {
                                viewModel.selectMessage(message)
                            }
                        is SurveyMessage ->
                            IndividualMessageItem(
                                isSelected = isSelected,
                                isNotificationEnabled = !message.isRead,
                                text = message.questions.count().toString(),
                                icon = R.drawable.quiz_icon,
                                messageTypeText = stringResource(R.string.quiz)
                            ) {
                                viewModel.selectMessage(message)
                            }
                        is AnnouncementMessage ->
                            IndividualMessageItem(
                                isSelected = isSelected,
                                isNotificationEnabled = !message.isRead,
                                text = stringResource(R.string.announcement),
                                icon = R.drawable.individual_message_icon,
                                messageTypeText = stringResource(R.string.message)
                            ) {
                                viewModel.selectMessage(message)
                            }
                        else -> throw IllegalStateException("Unsupported type: $message")
                    }
                }
            }
        }
    }
}