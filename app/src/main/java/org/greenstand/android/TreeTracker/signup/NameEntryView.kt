package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.registerForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.TextButton

@Composable
fun NameEntryView(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val uiState by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = registerForActivityResult(
        contract = CaptureImageContract(),
        onResult = {
            scope.launch {
                if (viewModel.setPhotoPath(it)) {
                    navController.navigate(NavRoute.Dashboard.route) {
//                        // TODO fix popup behavior to match app flow
                        popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    )

    Scaffold( // TODO: This scaffold should be moved to a host view so we don't have to keep replacing it
        topBar = {
            ActionBar(
                centerAction = { Text(stringResource(id = R.string.treetracker)) },
                rightAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.language,
                        onClick = { navController.navigate(NavRoute.Language.create(isFromTopBar = true)) }
                    )
                }
            )
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // TODO disable button until input fields are valid
                Button(
                    onClick = {
                        cameraLauncher.launch(true)
                    }
                ) {
                    Text("Next")
                }
            }
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
