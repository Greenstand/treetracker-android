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
package org.greenstand.android.TreeTracker.messages


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.RoundedImageContainer
import org.greenstand.android.TreeTracker.view.RoundedLocalImageContainer

private const val ConversationTestTag = "ConversationTestTag"

@Composable
fun ChatScreen(
    userId: Long,
    otherChatIdentifier: String,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            userId,
            otherChatIdentifier
        )
    )
) {
    val scrollState = rememberLazyListState()
    val state by viewModel.state.observeAsState(ChatState())
    val navController: NavHostController = LocalNavHostController.current

    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                    OtherChatIcon(state.from)
                },
                centerAction = {
                    Image(
                        modifier = Modifier.align(Alignment.Center),
                        painter = painterResource(id = R.drawable.chat_icon),
                        contentDescription = null,
                    )
                },
                rightAction = {
                    state.currentUser?.photoPath?.let {
                        RoundedLocalImageContainer(
                            imagePath = it,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            )
        },
        bottomBar = {
            ActionBar(
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Messages(
                state = state,
                modifier = Modifier.weight(1f),
                scrollState = scrollState,
                viewModel = viewModel
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 80.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    value = state.draftText,
                    onValueChange = { text -> viewModel.updateDraftText(text) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Go,
                        autoCorrect = false,
                    ),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.click_to_write_message),
                            color = Color.White
                        )
                    },
                    keyboardActions = KeyboardActions(
                        onGo = {
                            viewModel.sendMessage()
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = AppColors.LightGray,
                        backgroundColor = AppColors.DeepGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun BoxScope.OtherChatIcon(text: String) {
    RoundedImageContainer(
        modifier = Modifier
            .align(Alignment.Center)
            .background(color = AppColors.MediumGray)
    ) {
        Text(
            text = text.uppercase(),
            color = CustomTheme.textColors.lightText,
            fontWeight = FontWeight.Bold,
            style = CustomTheme.typography.regular,
        )
    }
}

@Composable
fun Messages(
    state: ChatState,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel
) {
    val messages = state.messages
    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            contentPadding = PaddingValues(top = 10.dp),
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
        ) {
            items(messages.size) { index ->
                Message(
                    msg = messages[index],
                    isOtherUser = viewModel.checkIsOtherUser(index),
                    isFirstMessageByAuthor = viewModel.checkChatAuthor(index, true),
                    isLastMessageByAuthor = viewModel.checkChatAuthor(index, true)
                )
            }
        }
    }
}

@Composable
fun Message(
    msg: DirectMessage,
    isOtherUser: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean
) {
    val spaceBetweenAuthors =
        if (isLastMessageByAuthor) Modifier.padding(bottom = 8.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        Column(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        ) {
            ChatItemBubble(msg, isOtherUser)
            if (isFirstMessageByAuthor) {
                // Last bubble before next author
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Between bubbles
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ChatItemBubble(
    message: DirectMessage,
    isOtherUser: Boolean,
) {
    val modifier: Modifier = if (isOtherUser) {
        Modifier
            .padding(start = 10.dp, end = 60.dp)
            .background(
                color = AppColors.MessageReceivedBackground,
                shape = RoundedCornerShape(6.dp)
            )
    } else {
        Modifier
            .padding(start = 60.dp, end = 10.dp)
            .background(
                color = AppColors.MessageAuthorBackground,
                shape = RoundedCornerShape(6.dp)
            )
    }

    val horizontalAlignment = if (isOtherUser) {
        Alignment.Start
    } else {
        Alignment.End
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = message.body,
            color = CustomTheme.textColors.lightText,
            fontWeight = FontWeight.Bold,
            style = CustomTheme.typography.regular,
        )
    }
}
