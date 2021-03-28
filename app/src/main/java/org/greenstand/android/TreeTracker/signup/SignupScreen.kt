package org.greenstand.android.TreeTracker.signup

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.registerForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.camera.Camera
import org.greenstand.android.TreeTracker.camera.CameraControl
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

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
                    navController.navigate(NavRoute.Dashboard.route)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TreeTracker") },
                actions = {
                    Text(
                        text = "Language",
                        modifier = Modifier
                            .clickable(onClick = {
                                navController.navigate(NavRoute.Language.create(isFromTopBar = true))
                            })
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
                Button(onClick = {
                    cameraLauncher.launch(true)
//                    navController.navigate(NavRoute.Camera.create(isSelfieMode = true))
                }) {
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
                value = state.emailPhone ?: "",
                onValueChange = { viewModel.setEmailPhone(it) },
                placeholder = { Text(text = "Phone/Email") }
            )
            TextField(
                value = state.name ?: "",
                onValueChange = { viewModel.setName(it) },
                placeholder = { Text(text = "Name") }
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
