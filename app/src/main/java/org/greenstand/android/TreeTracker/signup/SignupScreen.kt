package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.registerForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.ui.colorPrimary
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.TreeTrackerTextButton

@Composable
fun SignupFlow(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.state.observeAsState(SignUpState())
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

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { Text("Treetracker", color = colorPrimary) },
                rightAction = {
                    TreeTrackerTextButton(
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

            Row( // TODO: Make sure that the button changes colors when it's clicked.
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        // TODO: update viewModel with selected credential type
                    }
                ) {
                    Text(
                        text = "Email",
                        color = Color.Black
                    )
                }

                Button(
                    onClick = {
                        // TODO: update viewModel with selected credential type
                    }
                ) {
                    Text(
                        text = "Phone",
                        color = Color.Black
                    )
                }
            }

            if (state.showEmailText) {
                BorderedTextField( // FixMe: The highlight for the textfield is still green! This should be fixed! OR! Make it highlight green by just using an OutlinedTextField
                    value = state.emailText,
                    onValueChangeListener = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
                    placeholder = { Text(text = "Email", color = Color.White) } // TODO: throw this into a strings.xml
                )
            }

            if (state.showPhoneText) {
                OutlinedTextField(
                    modifier = Modifier.padding(top = 16.dp),
                    value = state.phoneText,
                    onValueChange = { updatedPhone -> viewModel.updatePhone(updatedPhone) },
                    label = { Text(text = "Phone", color = Color.White) } // TODO: throw this into a strings.xml
                )
            }

            TextField(
                modifier = Modifier.padding(top = 16.dp),
                value = state.name ?: "",
                onValueChange = { updatedName -> viewModel.updateName(updatedName) },
                placeholder = { Text(text = "Name", color = Color.White) } // TODO: throw this into a strings.xml
            )
        }
    }
}

@Composable
fun EnterPhoneEmail(
    emailPhone: String,
    onNavForward: () -> Unit,
    onNavBackward: () -> Unit,
    onNavLanguage: () -> Unit,
    onEmailPhoneChanged: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TreeTracker") },
                actions = {
                    Text(
                        text = "Language",
                        modifier = Modifier
                            .clickable(onClick = onNavLanguage)
                            .padding(8.dp)
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
                Button(onClick = onNavForward) {
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
            TextField(
                value = emailPhone,
                onValueChange = onEmailPhoneChanged,
                placeholder = { Text(text = "Phone / Email") }
            )
        }
    }
}

@Composable
fun EnterName(
    name: String,
    onNameChanged: (String) -> Unit,
    onNavForward: () -> Unit,
    onNavBackward: () -> Unit,
    onNavLanguage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TreeTracker") },
                actions = {
                    Text(
                        text = "Language",
                        modifier = Modifier
                            .clickable(onClick = onNavLanguage)
                            .padding(8.dp)
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
                Button(onClick = onNavForward) {
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
            TextField(
                value = name,
                onValueChange = onNameChanged,
                placeholder = { Text(text = "Name") }
            )
        }
    }
}

@Preview
@Composable
fun SignupScreen_Preview(@PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel) {
    SignupFlow(viewModel = viewModel)
}
