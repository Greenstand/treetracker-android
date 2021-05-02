package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigate
import androidx.navigation.compose.popUpTo
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.*

@Composable
fun NameEntryView(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val uiState by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { photoPath ->
        scope.launch {
            if (viewModel.setPhotoPath(photoPath)) {
                navController.navigate(NavRoute.Dashboard.route) {
//                        // TODO fix popup behavior to match app flow
                    popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { Text(stringResource(id = R.string.treetracker)) },
                rightAction = {
                    DepthButton(
                        onClick = {
                            navController.navigate(NavRoute.Language.create(true))
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = stringResource(id = R.string.language))
                    }
                }
            )
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    DepthButton(
                        onClick = {
                            cameraLauncher.launch(true)
                        },
                        modifier = Modifier.align(Alignment.Center).size(62.dp, 62.dp),
                        colors = AppButtonColors.ProgressGreen
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_right),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(AppColors.GrayShadow)
                        )
                    }
                }
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            BorderedTextField(
                value = uiState.name ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.name_placeholder), color = Color.White) }
            )
        }
    }
}
