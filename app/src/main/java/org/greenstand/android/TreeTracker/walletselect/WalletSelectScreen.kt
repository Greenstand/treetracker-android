package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.*


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
                    state.currentUser?.photoPath?.let {
                        LocalImage(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .padding(start = 15.dp,
                                         top= 10.dp,
                                         end =10.dp,
                                         bottom = 10.dp)
                                .aspectRatio(1.0f)
                                .clip(RoundedCornerShape(percent = 10))
                                .clickable { navController.navigate(NavRoute.UserSelect.route) },
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
                // Disabled for now. 2.0 will not have this feature.
//                centerAction = {
//                    OrangeAddButton(
//                        modifier = Modifier.align(Alignment.Center),
//                        onClick = { navController.navigate(NavRoute.SignupFlow.route) },
//                    )
//                },
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
                .padding(start = 10.dp,
                         top= 10.dp,
                         end =10.dp,
                         bottom = 90.dp)
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
                            .padding(start = 20.dp,
                                     top= 10.dp,
                                     end =10.dp,
                                     bottom = 30.dp)
            )

            UserButton(
                user = user,
                isSelected = isSelected,
                AppButtonColors.Default,
                AppColors.Green,
                onClick = { onClick(user.id) },
                )
        }
    }

}
