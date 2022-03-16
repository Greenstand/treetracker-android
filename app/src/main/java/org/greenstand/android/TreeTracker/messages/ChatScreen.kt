package org.greenstand.android.TreeTracker.messages


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.UserImageButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: Long,
    otherChatIdentifier: String,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(userId,otherChatIdentifier))
) {
    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    val state by viewModel.state.observeAsState(ChatState())


    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                   DepthButton(onClick = { /*TODO*/ }, modifier = Modifier
                       .width(100.dp)
                       .height(100.dp)
                       .padding(
                           start = 15.dp,
                           top = 10.dp,
                           end = 10.dp,
                           bottom = 10.dp
                       )
                       .aspectRatio(1.0f)
                       .clip(RoundedCornerShape(10.dp))) {
                       Box(modifier = Modifier
                           .padding(bottom = 12.dp, end = 1.dp)
                           .fillMaxSize()
                           .clip(RoundedCornerShape(10.dp))
                           .background(color = AppColors.MediumGray),contentAlignment = Alignment.Center){
                       Text(
                           text = stringResource(id = R.string.admin_placeholder).uppercase(),
                           color = CustomTheme.textColors.lightText,
                           fontWeight = FontWeight.Bold,
                           style = CustomTheme.typography.regular,
                       )
                       }
                   }
                },
                centerAction = {
                    Image(
                        painter = painterResource(id = R.drawable.chat_icon),
                        contentDescription = null,
                    )
                },
                rightAction = {
                    state.currentUser?.photoPath?.let {
                        UserImageButton(
                            onClick = {

                            },
                            imagePath = it
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
                        }
                    )
                }
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                Messages(
                    messages = state.messages,
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState
                )
            Box(modifier = Modifier
                .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 80.dp)
                .fillMaxWidth()
                .wrapContentHeight()){
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

const val ConversationTestTag = "ConversationTestTag"

@Composable
fun Messages(
    messages: List<DirectMessage>,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        val authorAdmin = "admin"
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            contentPadding = PaddingValues(top = 10.dp),
            // Add content padding so that the content can be scrolled (y-axis)
            // below the status bar + app bar
            // TODO: Get height from somewhere
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
        ) {

            for (index in messages.indices) {
                val prevAuthor = messages.getOrNull(index - 1)?.from
                val nextAuthor = messages.getOrNull(index + 1)?.from
                val content = messages[index]
                val isFirstMessageByAuthor = prevAuthor != content.from
                val isLastMessageByAuthor = nextAuthor != content.from
                item {
                    Message(
                        msg = content,
                        isAdmin = content.from == authorAdmin,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor
                    )
                }
            }
        }
    }
}

@Composable
fun Message(
    msg: DirectMessage,
    isAdmin: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean
) {
    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        TextMessage(
            msg = msg,
            isAdmin = isAdmin,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        )
    }
}

@Composable
fun TextMessage(
    msg: DirectMessage,
    isAdmin: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ChatItemBubble(msg, isAdmin)
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


@Composable
fun ChatItemBubble(
    message: DirectMessage,
    isAdmin: Boolean,
    ) {

    val modifier: Modifier = if(isAdmin) Modifier
        .padding(start = 10.dp, end = 60.dp)
        .background(color = AppColors.MessageReceivedBackground, shape = RoundedCornerShape(6.dp)) else Modifier
        .padding(start = 60.dp, end = 10.dp)
        .background(color = AppColors.MessageAuthorBackground, shape = RoundedCornerShape(6.dp))
    val horizontalAlignment = if (isAdmin) Alignment.Start else Alignment.End
    Column(modifier = modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(8.dp),horizontalAlignment = horizontalAlignment) {
            Text(
                text = message.body,
                color = CustomTheme.textColors.lightText,
                fontWeight = FontWeight.Bold,
                style = CustomTheme.typography.regular,
            )
    }
}
