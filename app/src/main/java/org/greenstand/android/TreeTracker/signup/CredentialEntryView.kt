package org.greenstand.android.TreeTracker.signup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.Constants
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.AppColors.Green
import org.greenstand.android.TreeTracker.view.ApprovalButton
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.CustomDialog
import org.greenstand.android.TreeTracker.view.CustomSnackbar
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.greenstand.android.TreeTracker.view.UserButton
@Composable
fun CredentialEntryView(viewModel: SignupViewModel, state: SignUpState) {
    val navController = LocalNavHostController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { TopBarTitle() },
                rightAction = { LanguageButton() }
            )
        },
        bottomBar = {
            if (state.showPrivacyDialog == false) {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true) {
                            navController.popBackStack()
                        }
                    },
                    rightAction = {
                        ArrowButton(
                            isLeft = false,
                        ) {
                            if (state.isCredentialValid) {
                                viewModel.submitInfo()
                            } else {
                                when (state.credential) {
                                    is Credential.Email -> {
                                        scope.launch {
                                            snackBarHostState.showSnackbar(
                                                message = context.getString(R.string.email_validation_error)
                                            )
                                        }
                                    }
                                    is Credential.Phone -> {
                                        scope.launch {
                                            snackBarHostState.showSnackbar(
                                                message = context.getString(R.string.phone_validation_error)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        if (state.existingUser != null) {
            ExistingUserDialog(viewModel = viewModel, navController = navController, state = state)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp)
        ) {
            val navigateToWebPage: () -> Unit = {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(Constants.TREE_TRACKER_URL)
                startActivity(context, intent, null)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PhoneCredentialButton(state, viewModel)
                EmailCredentialButton(state, viewModel)
            }

            when (state.credential) {
                is Credential.Email -> EmailTextField(state, viewModel, focusRequester, snackBarHostState,scope,context)
                is Credential.Phone -> PhoneTextField(state, viewModel, focusRequester, snackBarHostState,scope,context)
            }

            CustomSnackbar(snackbarHostState = snackBarHostState, backGroundColor = AppColors.Red)

            ViewWebMapText(isVisible = state.isInternetAvailable, onClick = navigateToWebPage)
        }
        if (state.showPrivacyDialog == true) {
            PrivacyPolicyDialog(viewModel = viewModel)
        }
    }
}

@Composable
fun EmailCredentialButton(state: SignUpState, viewModel: SignupViewModel) {
    CredentialButton(
        credential = state.credential,
        credentialType = Credential.Email::class.java,
        placeholderTextRes = R.string.email_placeholder,
        onClick = {
            viewModel.updateCredentialType(Credential.Email())
        }
    )
}

@Composable
fun PhoneCredentialButton(state: SignUpState, viewModel: SignupViewModel) {
    CredentialButton(
        credential = state.credential,
        credentialType = Credential.Phone::class.java,
        placeholderTextRes = R.string.phone_placeholder,
        onClick = {
            viewModel.updateCredentialType(Credential.Phone())
        }
    )
}

@Composable
fun ViewWebMapText(isVisible: Boolean, onClick: () -> Unit) {
    if (isVisible) {
        Text(
            text = stringResource(id = R.string.viewLiveWebMap),
            Modifier
                .padding(top = 10.dp)
                .clickable(onClick = onClick),
            color = Color.White,
            style = TextStyle(textDecoration = TextDecoration.Underline),
        )
    }
}

@Composable
private fun EmailTextField(state: SignUpState, viewModel: SignupViewModel, focusRequester: FocusRequester, snackBarHostState: SnackbarHostState, scope: CoroutineScope, context: Context) {
    val focusManager = LocalFocusManager.current
    BorderedTextField(
        value = state.email ?: "",
        padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        onValueChange = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
        placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Go,
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                focusManager.clearFocus()
                if (state.isCredentialValid) {
                    viewModel.submitInfo()
                } else {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.email_validation_error)
                        )
                    }
                }
            }
        ),
        onFocusChanged = { if (it.isFocused) viewModel.enableAutofocus() },
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled
    )
}

@Composable
private fun PhoneTextField(state: SignUpState, viewModel: SignupViewModel, focusRequester: FocusRequester,snackBarHostState: SnackbarHostState, scope: CoroutineScope, context: Context) {
    val focusManager = LocalFocusManager.current
    BorderedTextField(
        value = state.phone ?: "",
        padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        onValueChange = { updatedPhone -> viewModel.updatePhone(updatedPhone) },
        placeholder = { Text(text = stringResource(id = R.string.phone_placeholder), color = Color.White) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                focusManager.clearFocus()
                if (state.isCredentialValid) {
                    viewModel.submitInfo()
                } else {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.phone_validation_error)
                        )
                    }
                }
            }
        ),
        onFocusChanged = { if (it.isFocused) viewModel.enableAutofocus() },
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled
    )
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
            .padding(end = 12.dp)
            .size(120.dp, 50.dp),
        onClick = onClick,
        colors = AppButtonColors.ProgressGreen,
        isSelected = credentialType.isInstance(credential)
    ) {
        Text(
            text = stringResource(id = placeholderTextRes).uppercase(),
            color = CustomTheme.textColors.darkText,
            fontWeight = FontWeight.Bold,
            style = CustomTheme.typography.regular,
        )
    }
}

@Composable
fun ExistingUserDialog(
    viewModel: SignupViewModel,
    navController: NavHostController,
    state: SignUpState
) {
    CustomDialog(
        title = stringResource(R.string.user_exists_header),
        textContent = stringResource(R.string.user_exists_message),
        onNegativeClick = {
            viewModel.closeExistingUserDialog()
        },
        content = {
            state.existingUser?.let { user ->
                UserButton(
                    user = user,
                    isSelected = false,
                    buttonColors = AppButtonColors.Default,
                    selectedColor = Green,
                    onClick = {
                        navController.navigate(NavRoute.WalletSelect.create()) {
                            popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(
    viewModel: SignupViewModel,
) {
    Column(
        modifier = Modifier
            .padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 40.dp)
            .fillMaxSize()
            .padding(2.dp)
            .border(1.dp, color = Green, shape = RoundedCornerShape(percent = 10))
            .clip(RoundedCornerShape(percent = 10))
            .background(color = AppColors.Gray)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.greenstand_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 20.dp, height = 20.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.privacy_policy),
                color = CustomTheme.textColors.primaryText,
                style = CustomTheme.typography.large,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(id = R.string.policy_text_blob),
            modifier = Modifier
                .padding(bottom = 15.dp)
                .weight(9f)
                .verticalScroll(rememberScrollState())
        )
        ApprovalButton(
            modifier = Modifier
                .weight(0.8f)
                .size(50.dp)
                .align(CenterHorizontally),
            onClick = {
                viewModel.closePrivacyPolicyDialog()
            },
            approval = true
        )
    }
}

@Preview
@Composable
fun SignupScreen_Preview(
    @PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel
) {
    CredentialEntryView(viewModel = viewModel, state = SignUpState())
}
