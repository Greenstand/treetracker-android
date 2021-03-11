package org.greenstand.android.TreeTracker.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.Padding
import org.greenstand.android.TreeTracker.utilities.isValidEmail
import org.greenstand.android.TreeTracker.utilities.isValidPhoneNumber
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@Composable
fun LoginView(
    viewModel: LoginViewModel,
    navController: NavController
) {

    val uiScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val phoneTextInputState = mutableStateOf("")
    val emailTextInputState = mutableStateOf("")
    val emailFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val textInputService = LocalTextInputService.current

    viewModel.uiEvents.observe(LocalLifecycleOwner.current) { uiEvent ->
        when (uiEvent) {
            is LoginViewModel.UIEvent.NavigationRequestEvent -> navController.navigate(uiEvent.newNavDirection)
            is LoginViewModel.UIEvent.ErrorEvent -> uiScope.launch {
                snackBarHostState.showSnackbar(message = context.getString(uiEvent.errorMessage))
            }
            else -> Unit // **Note: Some UIEvents are for the LoginFragment to handle and should be ignored here
        }
    }

    TreeTrackerTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState) { data ->
                    Snackbar(snackbarData = data)
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 19.dp)
                    .fillMaxSize()
            ) {

                Image(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .width(dimensionResource(id = R.dimen.logo_width))
                        .height(dimensionResource(id = R.dimen.logo_height)),

                    imageVector = ImageVector.vectorResource(id = R.drawable.logo),
                    contentDescription = stringResource(id = R.string.greenstand_welcome_text)
                )

                PhoneInputText(
                    textInputState = phoneTextInputState,
                    onActionDone = {
                        emailFocusRequester.requestFocus()
                    }
                )

                EmailInputText(
                    textInputState = emailTextInputState,
                    onActionGo = {
                        emailFocusRequester.freeFocus()
                        val credentials = if (emailTextInputState.value.isNotBlank()) {
                            emailTextInputState.value
                        } else {
                            phoneTextInputState.value
                        }

                        viewModel.loginButtonClicked(credentials)

                        textInputService?.hideSoftwareKeyboard()
                    }
                )

                LoginButton(
                    emailTextState = emailTextInputState,
                    phoneTextState = phoneTextInputState,
                    onClick = {
                        val credentials = if (emailTextInputState.value.isNotBlank()) {
                            emailTextInputState.value
                        } else {
                            phoneTextInputState.value
                        }

                        viewModel.loginButtonClicked(credentials)
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneInputText(
    textInputState: MutableState<String>,
    onActionDone: KeyboardActionScope.() -> Unit
) {
    TextField(
        modifier = Modifier
            .padding(top = 28.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        value = textInputState.value,
        onValueChange = { newInput ->
            textInputState.value = newInput
        },
        label = {
            Text(text = stringResource(id = R.string.phone_text_hint))
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.phone),
                contentDescription = stringResource(id = R.string.content_description_phone)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = onActionDone
        )

    )
}

@Composable
private fun EmailInputText(
    textInputState: MutableState<String>,
    onActionGo: KeyboardActionScope.() -> Unit
) {
    TextField(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        value = textInputState.value,
        onValueChange = { newInput ->
            textInputState.value = newInput
        },
        label = {
            Text(text = stringResource(id = R.string.email_text_hint))
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.email),
                contentDescription = stringResource(id = R.string.content_description_email)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = onActionGo
        )

    )
}

@Composable
private fun LoginButton(
    emailTextState: MutableState<String>,
    phoneTextState: MutableState<String>,
    onClick: () -> Unit
) {
    TreeTrackerButton(
        onClick = onClick,
        textRes = R.string.login_button_title,
        padding = Padding(top = 18.dp),
        isEnabled = emailTextState.value.trim().isValidEmail() || phoneTextState.value.trim().isValidPhoneNumber()
    )
}

@Composable
@Preview
private fun Login_Preview() {
    // TODO: Build the PreviewProvider for this view.
}
