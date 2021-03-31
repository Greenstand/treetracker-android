package org.greenstand.android.TreeTracker.userselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TextButton

@Composable
fun UserSelectScreen(viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)) {

    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(UserSelectState())

    Scaffold(
        topBar = {
            ActionBar(
                rightAction = {
                    LanguageButton()
                }
            )
        },
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
                                navController.navigate(NavRoute.WalletSelect.create(it)) }
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

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("User Select")
            state.planters.forEach { user ->
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            viewModel.selectUser(user)
                        }
                        .background(color = if (state.selectedPlanter?.id == user.id) Color.Gray else Color.White)
                )
            }
        }
    }
}