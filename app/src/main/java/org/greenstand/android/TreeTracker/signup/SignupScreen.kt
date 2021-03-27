package org.greenstand.android.TreeTracker.signup

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
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

    val parentNavController = LocalNavHostController.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    NavHost(navController, startDestination = "phoneEmail") {

        composable("phoneEmail") {
            EnterPhoneEmail(
                emailPhone = state.emailPhone ?: "",
                onNavForward = { navController.navigate("name") },
                onNavBackward = { navController.popBackStack() },
                onNavLanguage = { parentNavController.navigate("language/true") },
                onEmailPhoneChanged = { viewModel.setEmailPhone(it) }
            )
        }

        composable("name") {
            EnterName(
                name = state.name ?: "",
                onNameChanged = { viewModel.setName(it) },
                onNavForward = {
                    parentNavController.navigate(NavRoute.DashboardView.route) {
                        launchSingleTop = true
                        popUpTo(NavRoute.SplashScreen.route) { inclusive = true }
                    }
                               },
                onNavBackward = { navController.popBackStack() },
                onNavLanguage = { parentNavController.navigate("language/true") },
            )
        }

        composable("camera") {

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
