package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = LocalViewModelFactory.current),
    navController: NavController = LocalNavHostController.current
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TreeTracker") },
                navigationIcon = {
                    Text(
                        text = "Org",
                        modifier = Modifier.clickable {
                            navController.navigate(DashboardFragmentDirections.actionGlobalOrgPickerFragment())
                        }
                    )
                },
                actions = {
                    Text(
                        text = "Language",
                        modifier = Modifier.clickable {
                            navController.navigate(DashboardFragmentDirections.actionGlobalLanguagePickerFragment())
                        }
                    )
                }
            )
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
                    text = "Upload",
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    navController.navigate(DashboardFragmentDirections.actionDashboardFragmentToUserSelectFragment())
                }
            ) {
                Text(
                    text = "Track",
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(
                    text = "Messages",
                )
            }
        }
    }
}

@Preview
@Composable
fun DashboardScreen_Preview(@PreviewParameter(DashboardPreviewParameter::class) viewModel: DashboardViewModel) {
    DashboardScreen(viewModel = viewModel, rememberNavController())
}
