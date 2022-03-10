package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.IndividualMessageButton
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.UserImageButton

@ExperimentalFoundationApi
@Composable
fun IndividualMessageListScreen(
    planterInfoId: Long,
    viewModel: IndividualMessageListViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(IndividualMessageListState())

    LaunchedEffect(true) {
        viewModel.loadPlanter(planterInfoId)
    }


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
                        isEnabled = false,
                        colors = AppButtonColors.MessagePurple,
                        onClick = { }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = AppButtonColors.MessagePurple,
                        onClick = {
                            navController.navigate(NavRoute.Dashboard.route) {
                                popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
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
                IndividualMessageButton(
                    isSelected = true,
                    message = message,
                    isNotificationEnabled = true,
                    selectedColor = AppColors.Purple
                ) {  }
            }
        }
    }
}