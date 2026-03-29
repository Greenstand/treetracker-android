/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.NoMessages
import org.greenstand.android.TreeTracker.view.UserImageButton

@ExperimentalFoundationApi
@Composable
fun IndividualMessageListScreen(
    userId: Long,
    viewModel: IndividualMessageListViewModel = viewModel(factory = IndividualMessageListViewModelFactory(userId))
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState()

    IndividualMessageList(
        state = state,
        onHandleAction = { action ->
            when (action) {
                is IndividualMessageListAction.NavigateBack -> {
                    navController.popBackStack()
                }
                is IndividualMessageListAction.NavigateToSelected -> {
                    when (val msg = state.selectedMessage) {
                        is DirectMessage -> navController.navigate(ChatRoute(planterInfoId = userId, otherChatIdentifier = msg.from))
                        is SurveyMessage -> navController.navigate(SurveyRoute(messageId = msg.id))
                        is AnnouncementMessage -> navController.navigate(AnnouncementRoute(messageId = msg.id))
                    }
                }
                else -> viewModel.handleAction(action)
            }
        },
    )
}

@ExperimentalFoundationApi
@Composable
fun IndividualMessageList(
    state: IndividualMessageListState = IndividualMessageListState(),
    onHandleAction: (IndividualMessageListAction) -> Unit = {},
) {
    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                leftAction = {
                    state.currentUser?.photoPath?.let {
                        UserImageButton(
                            onClick = { onHandleAction(IndividualMessageListAction.NavigateBack) },
                            imagePath = it
                        )
                    }
                }
            )
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedMessage != null,
                        colors = AppButtonColors.MessagePurple,
                        onClick = { onHandleAction(IndividualMessageListAction.NavigateToSelected) }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = AppButtonColors.MessagePurple,
                        onClick = { onHandleAction(IndividualMessageListAction.NavigateBack) }
                    )
                }
            )
        }
    ) {
        if (state.messages.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(it),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = it.calculateBottomPadding())
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
                                    onHandleAction(IndividualMessageListAction.SelectMessage(message))
                                }
                            is SurveyMessage ->
                                IndividualMessageItem(
                                    isSelected = isSelected,
                                    isNotificationEnabled = !message.isRead,
                                    text = message.title,
                                    icon = R.drawable.quiz_icon,
                                    messageTypeText = stringResource(R.string.survey)
                                ) {
                                    onHandleAction(IndividualMessageListAction.SelectMessage(message))
                                }
                            is AnnouncementMessage ->
                                IndividualMessageItem(
                                    isSelected = isSelected,
                                    isNotificationEnabled = !message.isRead,
                                    text = message.subject,
                                    icon = R.drawable.announcement_icon,
                                    messageTypeText = stringResource(R.string.announcement),
                                    iconPadding = PaddingValues(bottom = 30.dp)
                                ) {
                                    onHandleAction(IndividualMessageListAction.SelectMessage(message))
                                }
                            else -> throw IllegalStateException("Unsupported type: $message")
                        }
                    }
                }
            }
        } else {
            NoMessages()
        }
    }
}
