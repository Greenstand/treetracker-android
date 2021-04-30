package org.greenstand.android.TreeTracker.userselect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.*

@ExperimentalFoundationApi
@Composable
fun UserSelectScreen(
    viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(UserSelectState())

    Scaffold(
        bottomBar = {
            ActionBar(
                centerAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.create_user,
                        onClick = { navController.navigate(NavRoute.SignupFlow.route) }
                    )
                },
                rightAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.next,
                        enabled = state.selectedPlanter != null,
                        onClick = {
                            state.selectedPlanter?.id?.let {
                                navController.navigate(NavRoute.WalletSelect.create(it))
                            }
                        }
                    )
                },
                leftAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.back,
                        onClick = { navController.popBackStack() }
                    )
                }
            )
        }
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        ) {
            items(state.planters) { user ->
                UserButton(
                    user = user,
                    state = state,
                ) { viewModel.selectUser(user) }
            }
        }

        //        Column(
        //            verticalArrangement = Arrangement.Center,
        //            horizontalAlignment = Alignment.CenterHorizontally
        //        ) {
        //            Text("User Select")
        //            state.planters.forEach { user ->
        //                Text(
        //                    text = "${user.firstName} ${user.lastName}",
        //                    modifier = Modifier
        //                        .padding(16.dp)
        //                        .clickable {
        //                            viewModel.selectUser(user)
        //                        }
        //                        .background(color = if (state.selectedPlanter?.id == user.id) Color.Gray else Color.White)
        //                )
        //            }
        //        }
    }
}

@Composable
fun UserButton(
    user: PlanterInfoEntity,
    state: UserSelectState,
    onClick: () -> Unit
) {
    val isSelected = state.selectedPlanter?.id == user.id

    DepthButton(
        onClick = onClick,
        isSelected = isSelected,
        modifier = Modifier
            .size(height = 220.dp, width = 156.dp)
            .padding(8.dp),
        colors = DepthButtonColors(
            color = AppColors.Gray,
            shadowColor = if (isSelected) AppColors.GreenShadow else AppColors.GrayShadow,
            disabledColor = AppColors.GrayShadow,
            disabledShadowColor = AppColors.GrayShadow
        )
    ) {
        Text(text = "${user.firstName}\n${user.lastName}")
    }
}
