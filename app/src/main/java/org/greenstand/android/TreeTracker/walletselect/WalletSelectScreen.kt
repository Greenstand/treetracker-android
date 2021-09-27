package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.NonDisposableHandle.parent
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.*
import org.greenstand.android.TreeTracker.R
import kotlin.Lazy


@Composable
fun WalletSelectScreen(
    planterInfoId: Long,
    viewModel: WalletSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val state by viewModel.state.observeAsState(initial = WalletSelectState())

    LaunchedEffect(true) {
        viewModel.loadPlanter(planterInfoId)
    }

    val navController = LocalNavHostController.current

    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                    state.selectedUser?.photoPath?.let {
                        LocalImage(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .padding(15.dp, 10.dp, 10.dp, 10.dp)
                                .aspectRatio(1.0f)
                                .clip(RoundedCornerShape(percent = 10)),
                            imagePath = it,
                            contentScale = ContentScale.Crop,
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
                        isEnabled = state.selectedUser != null
                    ) {
                        state.currentUser?.let { user ->
                            viewModel.startSession(user)
                            navController.navigate(NavRoute.TreeCapture.create(user.photoPath))
                        }
                    }
                },
                centerAction = {
                        DepthButton(
                            onClick = { navController.navigate(NavRoute.SignupFlow.route) },
                            shape = DepthSurfaceShape.Circle,
                            colors = AppButtonColors.UploadOrange,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(height = 70.dp, width = 70.dp),
                        ) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Black,fontWeight = FontWeight.Bold, fontSize = 50.sp)) {
                                        append("+")
                                    }

                                     }
                            )
                        }

                },
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                }
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(10.dp,10.dp,10.dp,90.dp),

        ) {
            state.currentUser?.let { currentUser ->
                item {
                    WalletItem(currentUser, state.selectedUser == currentUser) {
                        viewModel.selectPlanter(it)
                    }
                    }
            }
            state.alternateUsers?.let { alternateUsers ->
                items(alternateUsers) { user ->
                    WalletItem(user, state.selectedUser == user) {
                        viewModel.selectPlanter(it)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletItem(user: User, isSelected: Boolean, onClick: (Long) -> Unit) {


    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {

            val image: Painter = if (isSelected) painterResource(id = R.drawable.person_red) else painterResource(id = R.drawable.treetracker_logo)
            Image(
                painter = image,
                contentDescription = "",
                        modifier = Modifier
                            .height(150.dp)
                            .width(150.dp)
                            .padding(20.dp,10.dp,10.dp,30.dp,)



            )


            WalletUserButton(
                user = user,
                isSelected = isSelected,
                AppButtonColors.Default,
                AppColors.Green,
                onClick = { onClick(user.id) },

                )
        }
    }

}
