package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun NameEntryView(viewModel: SignupViewModel, state: SignUpState) {
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    TopBarTitle()
                },
                rightAction = {
                    LanguageButton()
                }
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
                        navController.navigate(NavRoute.Selfie.route)
                    }
                }
            )
        }
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp)
        ) {
            BorderedTextField(
                value = state.name ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.name_placeholder), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                    autoCorrect = false,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (state.name != null) {
                            navController.navigate(NavRoute.Selfie.route)
                        }
                    }
                )
            )
        }
    }
}