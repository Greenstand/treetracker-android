package org.greenstand.android.TreeTracker.userselect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonColors
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserSelect(
    navigationButtonColors: DepthButtonColors,
    isCreateUserEnabled: Boolean,
    isNotificationEnabled: Boolean,
    onNextRoute: (User) -> String,
) {
    val viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(UserSelectState())

    Scaffold(
        bottomBar = {
            ActionBar(
                centerAction = {
                    if (isCreateUserEnabled) {
                        OrangeAddButton(modifier = Modifier
                            .align(Alignment.Center),
                            onClick = { navController.navigate(NavRoute.SignupFlow.route)})
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedUser != null,
                        colors = navigationButtonColors,
                        onClick = {
                            state.selectedUser?.let {
                                navController.navigate(onNextRoute(it))
                            }
                        }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = navigationButtonColors,
                        onClick = {
                            navController.popBackStack()
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
            items(state.users) { user ->
                UserButton(
                    user = user,
                    isSelected = state.selectedUser?.id == user.id,
                    AppButtonColors.Default,
                    AppColors.Green
                ) { viewModel.selectUser(user) }
            }
        }
    }
}

@Composable
fun CreateUserButton() {
    DepthButton(
        onClick = { /*TODO*/ },
        shape = DepthSurfaceShape.Circle,
    ) {
        Text("+")
    }
}
