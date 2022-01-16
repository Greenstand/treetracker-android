package org.greenstand.android.TreeTracker.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors.GrayShadow
import org.greenstand.android.TreeTracker.view.AppColors.Green
import org.greenstand.android.TreeTracker.view.AppColors.GreenShadow
import org.greenstand.android.TreeTracker.view.AppColors.MediumGray
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun CredentialEntryView(viewModel: SignupViewModel, state: SignUpState) {
    val navController = LocalNavHostController.current
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { TopBarTitle() },
                rightAction = { LanguageButton() }
            )
        },
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = (state.isEmailValid || state.isPhoneValid)
                    ) {
                        viewModel.doesCredentialExist()
                    }
                }
            )
        }
    ) {
        if (state.existingUser != null) {
            ExistingUserDialog(viewModel = viewModel, navController = navController, state = state)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CredentialButton(
                    credential = state.credential,
                    credentialType = Credential.Email::class.java,
                    placeholderTextRes = R.string.email_placeholder,
                    onClick = {
                        viewModel.updateCredentialType(Credential.Email())
                    }
                )

                CredentialButton(
                    credential = state.credential,
                    credentialType = Credential.Phone::class.java,
                    placeholderTextRes = R.string.phone_placeholder,
                    onClick = {
                        viewModel.updateCredentialType(Credential.Phone())
                    }
                )
            }

            when (state.credential) {
                is Credential.Email -> BorderedTextField(
                    value = state.email ?: "",
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
                    placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                        autoCorrect = false,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            viewModel.doesCredentialExist()
                        }
                    ),
                    onFocusChanged = { if (it.isFocused) viewModel.enableAutofocus() },
                    focusRequester = focusRequester,
                    autofocusEnabled = state.autofocusTextEnabled
                )

                is Credential.Phone -> BorderedTextField(
                    value = state.phone ?: "",
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedPhone -> viewModel.updatePhone(updatedPhone) },
                    placeholder = { Text(text = stringResource(id = R.string.phone_placeholder), color = Color.White) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            viewModel.doesCredentialExist()
                        }
                    ),
                    onFocusChanged = { if (it.isFocused) viewModel.enableAutofocus() },
                    focusRequester = focusRequester,
                    autofocusEnabled = state.autofocusTextEnabled
                )
            }
        }
    }
}

@Composable
fun <T : Credential> CredentialButton(
    credential: Credential,
    credentialType: Class<T>,
    placeholderTextRes: Int,
    onClick: () -> Unit,
) {
    DepthButton(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(120.dp, 50.dp),
        onClick = onClick,
        colors = DepthButtonColors(
            color = if (credentialType.isInstance(credential)) {
                Green
            } else {
                MediumGray
            },
            shadowColor = if (credentialType.isInstance(credential)) {
                GreenShadow
            } else {
                GrayShadow
            },
            disabledColor = GrayShadow,
            disabledShadowColor = GrayShadow
        ),
        isSelected = credentialType.isInstance(credential)
    ) {
        Text(
            text = stringResource(id = placeholderTextRes),
            color = Color.Black,
        )
    }
}

@Composable
fun ExistingUserDialog(
    viewModel: SignupViewModel,
    navController: NavHostController,
    state: SignUpState
) {
    AlertDialog(
        onDismissRequest = {
            viewModel.closeExistingUserDialog()
        },
        title = {
            Text(text = stringResource(R.string.user_exists_header))
        },
        text = {
            Text(
                text = stringResource(R.string.user_exists_message),
                color = Color.Green
            )
        },
        buttons = {
            state.existingUser?.let { user ->
                UserButton(
                    user = user,
                    isSelected = false,
                    AppButtonColors.Default,
                    Green
                ) {
                    navController.navigate(NavRoute.WalletSelect.create(user.id)) {
                        popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = {
                    viewModel.closeExistingUserDialog()
                }
            ) {
                Text(
                    text = stringResource(R.string.cancel)
                )
            }
        }
    )
}

@Preview
@Composable
fun SignupScreen_Preview(
    @PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel
) {
    CredentialEntryView(viewModel = viewModel, state = SignUpState())
}
