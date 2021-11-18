package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.LanguageButton

@Composable
fun NameEntryView(viewModel: SignupViewModel, state: SignUpState) {
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { photoPath ->
        scope.launch {
            viewModel.createUser(photoPath)?.let { user ->
                if (user.isPowerUser) {
                    // In initial signup flow, clear stack and go to dashboard
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Language.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    // In tracking flow, clear login stack and go to wallet selection flow
                    navController.navigate(NavRoute.WalletSelect.create(user.id)) {
                        popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { Text(stringResource(id = R.string.treetracker)) },
                rightAction = { LanguageButton() }
            )
        },
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        viewModel.goToCredentialEntry()
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.name != null
                    ) {
                        cameraLauncher.launch(true)
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
                value = state.name ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.name_placeholder), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.moveFocus(FocusDirection.Down)
                        }
                )
            )
            BorderedTextField(
                value = state.organization ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedOrganization -> viewModel.updateOrganization(updatedOrganization) },
                placeholder = { Text(text = stringResource(id = R.string.organization), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (state.name != null) {
                            cameraLauncher.launch(true)
                        }
                    }
                )
            )
        }
    }
}
