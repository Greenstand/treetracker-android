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
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory

@Composable
fun SignupScreen(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current),
    onNavBackward: () -> Unit,
    onNavForward: () -> Unit,
    onNavLanguage: () -> Unit
) {
    val state by viewModel.state.observeAsState(SignUpState())

    val onBackPressedCallback: OnBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // viewModel.popScreen()
            }
        }
    }
    val dispatchOwner = LocalOnBackPressedDispatcherOwner.current
    DisposableEffect(key1 = true) {
        // dispatchOwner.onBackPressedDispatcher.addCallback(onBackPressedCallback)
        onDispose {
            // dispatchOwner.onBackPressedDispatcher
        }
    }

    Crossfade(targetState = state.screen) { screen ->
        when (screen) {
            SignupFlowScreen.EMAIL_PHONE -> EnterPhoneEmail(
                emailPhone = state.emailPhone ?: "",
                onNavForward = { viewModel.setScreen(SignupFlowScreen.NAME) },
                onNavBackward = onNavBackward,
                onNavLanguage = onNavLanguage,
                onEmailPhoneChanged = { viewModel.setEmailPhone(it) }
            )
            SignupFlowScreen.NAME -> EnterName(
                name = state.name ?: "",
                onNameChanged = { viewModel.setName(it) },
                onNavLanguage = onNavLanguage,
                onNavForward = onNavForward,
                onNavBackward = { viewModel.setScreen(SignupFlowScreen.EMAIL_PHONE) }
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
    SignupScreen(viewModel = viewModel, onNavBackward = { /*TODO*/ }, onNavForward = { /*TODO*/ }, onNavLanguage = { /*TODO*/ })
}
