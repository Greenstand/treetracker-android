package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TextButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val navController = LocalNavHostController.current
    Scaffold(
        topBar = {
            DashboardTopBar(navController)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(
                    text = "Upload"
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    navController.navigate(NavRoute.UserSelect.route)
                }
            ) {
                Text(
                    text = "Track"
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(
                    text = "Messages"
                )
            }
        }
    }
}

@Composable
fun DashboardTopBar(navController: NavController) {
    ActionBar(
        leftAction = {
            TextButton(
                modifier = Modifier.align(Alignment.Center),
                stringRes = R.string.organization,
                onClick = { navController.navigate(NavRoute.Org.route) }
            )
        },
        centerAction = { TopBarTitle() },
        rightAction = { LanguageButton() }
    )
}

@Preview
@Composable
fun DashboardScreen_Preview(
    @PreviewParameter(DashboardPreviewParameter::class) viewModel: DashboardViewModel
) {
    DashboardScreen(viewModel = viewModel)
}
