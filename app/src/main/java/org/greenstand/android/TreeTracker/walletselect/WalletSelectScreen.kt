package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.UserButton


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
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ActionBar(
                leftAction = {
                    state.currentUser?.photoPath?.let {
                        UserImageButton(
                            onClick = {
                                navController.navigate(NavRoute.UserSelect.route) {
                                    popUpTo(NavRoute.Dashboard.route)
                                    launchSingleTop = true
                                }
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
                        isEnabled = state.selectedUser != null
                    ) {
                        scope.launch {
                            state.currentUser?.let { user ->
                                viewModel.startSession()
                                navController.navigate(NavRoute.TreeCapture.create(user.photoPath))
                            }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = 90.dp
                )
        ) {
            state.currentUser?.let { currentUser ->
                item {
                    WalletItem(currentUser, state.selectedUser == currentUser) {
                        viewModel.selectPlanter(it)
                    }
                    }
            }
            state.alternateUsers.let { alternateUsers ->
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
            val image: Painter = painterResource(id = R.drawable.active_offer)
            val arrowImage: Painter = if (isSelected) painterResource(id = R.drawable.active_arrow) else painterResource(id = R.drawable.inactive_arrow)
            val modifier = if (!isSelected) Modifier.alpha(0.4f) else Modifier

            Image(
                painter = image,
                contentDescription = "",
                modifier = modifier
                    .height(140.dp)
                    .width(120.dp)
            )
            Image(
                painter = arrowImage,
                contentDescription = "",
                modifier = Modifier
                    .height(40.dp)
                    .width(65.dp)
                    .padding(
                        end = 20.dp,
                        start = 10.dp
                    )
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