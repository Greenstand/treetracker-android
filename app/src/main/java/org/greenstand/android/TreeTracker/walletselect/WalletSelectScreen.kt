package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
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
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.UserImageButton


@Composable
fun WalletSelectScreen(
    viewModel: WalletSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val state by viewModel.state.observeAsState(initial = WalletSelectState())

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
                            state.currentUser?.let {
                                navController.navigate(NavRoute.AddOrg.create())
                            }
                        }
                    }
                },
                centerAction = {
                    OrangeAddButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { navController.navigate(NavRoute.AddWallet.create()) },
                    )
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val image: Painter = painterResource(id = R.drawable.active_offer)
        val arrowImage: Painter =
            if (isSelected) painterResource(id = R.drawable.active_arrow) else painterResource(id = R.drawable.inactive_arrow)
        val modifier = if (!isSelected) Modifier.alpha(0.4f) else Modifier

        Image(
            painter = image,
            contentDescription = "",
            modifier = modifier
                .weight(2f)
                .height(140.dp)
        )
        Image(
            painter = arrowImage,
            contentDescription = "",
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .padding(
                    end = 20.dp,
                    start = 10.dp
                )
        )
        Box(modifier = Modifier.weight(2.6f)) {
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