package org.greenstand.android.TreeTracker.userselect

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@Composable
fun UserSelectScreen(viewModel: UserSelectViewModel, navController: NavController) {
    Scaffold {

        val users by viewModel.planterInfoList.observeAsState(emptyList())

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("User Select")
            users.forEach { user ->
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}