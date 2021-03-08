package org.greenstand.android.TreeTracker.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.Padding
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@Composable
fun LoginView() {

    val snackBarHostState = remember { SnackbarHostState() }

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
                LoginScreen()
            }
        }
    }
}

@Composable
private fun LoginScreen() {

    val phoneTextInputState = mutableStateOf("")
    val emailTextInputState = mutableStateOf("")

    Image(
        modifier = Modifier
            .padding(top = 28.dp)
            .width(dimensionResource(id = R.dimen.logo_width))
            .height(dimensionResource(id = R.dimen.logo_height)),

        imageVector = ImageVector.vectorResource(id = R.drawable.logo),
        contentDescription = stringResource(id = R.string.greenstand_welcome_text)
    )

    PhoneInputText(textInputState = phoneTextInputState)
    EmailInputText(textInputState = emailTextInputState)
    TreeTrackerButton(
        onClick = { /*TODO: Make call to viewmodel to start login behavior.*/ },
        textRes = R.string.login_button_title,
        padding = Padding(top = 18.dp)
    )
}

@Composable
private fun PhoneInputText(
    textInputState: MutableState<String>
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
            imeAction = ImeAction.Done // todo: add this action in here
        )

    )
}

@Composable
private fun EmailInputText(
    textInputState: MutableState<String>
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
            imeAction = ImeAction.Go // todo: add this action in here
        )

    )
}

@Composable
@Preview
private fun Login_Preview() {
    LoginView()
}
