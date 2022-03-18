package org.greenstand.android.TreeTracker.messages.announcementmessage


import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.Constants
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.CustomDialog
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.UserImageButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(
    userId: Long,
    otherChatIdentifier: String,
    viewModel: AnnouncementViewModel = viewModel(
        factory = AnnouncementViewModelFactory(
            userId,
            otherChatIdentifier
        )
    )
) {
    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    val state by viewModel.state.observeAsState(AnnouncementState())

    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                    OtherChatButton()
                },
                centerAction = {
                    Image(
                        painter = painterResource(id = R.drawable.chat_icon),
                        contentDescription = null,
                    )
                },
                rightAction = {
                    state.currentUser?.photoPath?.let { imagePath ->
                        LocalImage(
                            modifier = Modifier
                                .padding(bottom = 12.dp, end = 1.dp)
                                .background(shape = CircleShape, color = Color.Transparent)
                                .fillMaxSize()
                                .aspectRatio(1.0f),
                            imagePath = imagePath,
                            contentScale = ContentScale.Crop
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
                state = state,
                modifier = Modifier.weight(1f),
                scrollState = scrollState,
                viewModel = viewModel
            )
            if (state.showNoInternetDialog) {
                NoInternetDialog(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Messages(
    state: AnnouncementState,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: AnnouncementViewModel
) {
    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            contentPadding = PaddingValues(top = 10.dp),
            // Add content padding so that the content can be scrolled (y-axis)
            // below the status bar + app bar
            // TODO: Get height from somewhere
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(state.messages) { message ->
                ChatItemBubble(message = message, state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ChatItemBubble(
    message: AnnouncementMessage,
    state: AnnouncementState,
    viewModel: AnnouncementViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(start = 10.dp, end = 60.dp)
            .background(
                color = AppColors.MessageReceivedBackground,
                shape = RoundedCornerShape(6.dp)
            )
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp), horizontalAlignment = Alignment.Start
    ) {
        message.body?.let {
            Text(
                text = it,
                color = CustomTheme.textColors.lightText,
                fontWeight = FontWeight.Bold,
                style = CustomTheme.typography.regular,
            )
        }
        message.videoLink?.let {
            Text(
                text = it,
                Modifier
                    .padding(top = 10.dp)
                    .clickable(onClick = {
                        if (state.isInternetAvailable) {
                            openUrlLink(context = context, url = message.videoLink)
                        } else {
                            viewModel.updateNoInternetDialogState(true)
                        }
                    }),
                color = Color.White,
                style = TextStyle(textDecoration = TextDecoration.Underline),
            )
        }
    }
}

@Composable
fun OtherChatButton() {
    DepthButton(
        onClick = { /*TODO*/ }, modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .padding(
                start = 15.dp,
                top = 10.dp,
                end = 10.dp,
                bottom = 10.dp
            )
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp, end = 1.dp)
                .fillMaxSize()
                .background(color = AppColors.MediumGray, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.admin_placeholder).uppercase(),
                color = CustomTheme.textColors.lightText,
                fontWeight = FontWeight.Bold,
                style = CustomTheme.typography.regular,
            )
        }
    }
}

fun openUrlLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    ContextCompat.startActivity(context, intent, null)
}

@Composable
fun NoInternetDialog(viewModel: AnnouncementViewModel) {
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.error_outline),
        title = "No Internet Connection",
        textContent = "Kindly connect to a WI-fi or turn on mobile data to access this link",
        onPositiveClick = {
            viewModel.updateNoInternetDialogState(false)
        }
    )
}