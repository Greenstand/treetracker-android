/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.signup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.Constants
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.AppColors.Green
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.CustomSnackbar
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog
import org.greenstand.android.TreeTracker.view.dialogs.PrivacyPolicyDialog

@Composable
fun CredentialEntryView(
    state: SignUpState,
    onHandleAction: (SignupAction) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = { TopBarTitle() },
                rightAction = { LanguageButton() },
            )
        },
        bottomBar = {
            if (state.showPrivacyDialog == false) {
                ActionBar(
                    modifier = Modifier.navigationBarsPadding(),
                    leftAction = {
                        ArrowButton(isLeft = true) {
                            onHandleAction(SignupAction.NavigateBack)
                        }
                    },
                    rightAction = {
                        ArrowButton(
                            isLeft = false,
                        ) {
                            if (state.isCredentialValid) {
                                onHandleAction(SignupAction.SubmitInfo)
                            } else {
                                when (state.credential) {
                                    is Credential.Email -> {
                                        scope.launch {
                                            snackBarHostState.showSnackbar(
                                                message = context.getString(R.string.email_validation_error),
                                            )
                                        }
                                    }
                                    is Credential.Phone -> {
                                        scope.launch {
                                            snackBarHostState.showSnackbar(
                                                message = context.getString(R.string.phone_validation_error),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        },
    ) {
        if (state.existingUser != null) {
            ExistingUserDialog(
                state = state,
                onHandleAction = onHandleAction,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .verticalScroll(rememberScrollState()),
        ) {
            val navigateToWebPage: () -> Unit = {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(Constants.TREE_TRACKER_URL)
                startActivity(context, intent, null)
            }

            CustomSnackbar(snackbarHostState = snackBarHostState, backGroundColor = AppColors.Red)

            Row(
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                PhoneCredentialButton(state) { onHandleAction(SignupAction.UpdateCredentialType(it)) }
                EmailCredentialButton(state) { onHandleAction(SignupAction.UpdateCredentialType(it)) }
            }

            when (state.credential) {
                is Credential.Email ->
                    EmailTextField(
                        state,
                        onUpdateEmail = { onHandleAction(SignupAction.UpdateEmail(it)) },
                        onSubmitInfo = { onHandleAction(SignupAction.SubmitInfo) },
                        onEnableAutofocus = { onHandleAction(SignupAction.EnableAutofocus) },
                        focusRequester,
                        snackBarHostState,
                        scope,
                        context,
                    )
                is Credential.Phone ->
                    PhoneTextField(
                        state,
                        onUpdatePhone = { onHandleAction(SignupAction.UpdatePhone(it)) },
                        onSubmitInfo = { onHandleAction(SignupAction.SubmitInfo) },
                        onEnableAutofocus = { onHandleAction(SignupAction.EnableAutofocus) },
                        focusRequester,
                        snackBarHostState,
                        scope,
                        context,
                    )
            }

            ViewWebMapText(isVisible = state.isInternetAvailable, onClick = navigateToWebPage)
        }
        if (state.showPrivacyDialog == true) {
            PrivacyPolicyDialog(onDismiss = { onHandleAction(SignupAction.ClosePrivacyPolicyDialog) })
        }
    }
}

@Composable
fun EmailCredentialButton(
    state: SignUpState,
    onUpdateCredentialType: (Credential) -> Unit,
) {
    CredentialButton(
        credential = state.credential,
        credentialType = Credential.Email::class.java,
        placeholderTextRes = R.string.email_placeholder,
        onClick = {
            onUpdateCredentialType(Credential.Email())
        },
    )
}

@Composable
fun PhoneCredentialButton(
    state: SignUpState,
    onUpdateCredentialType: (Credential) -> Unit,
) {
    CredentialButton(
        credential = state.credential,
        credentialType = Credential.Phone::class.java,
        placeholderTextRes = R.string.phone_placeholder,
        onClick = {
            onUpdateCredentialType(Credential.Phone())
        },
    )
}

@Composable
fun ViewWebMapText(
    isVisible: Boolean,
    onClick: () -> Unit,
) {
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
private fun EmailTextField(
    state: SignUpState,
    onUpdateEmail: (String) -> Unit,
    onSubmitInfo: () -> Unit,
    onEnableAutofocus: () -> Unit,
    focusRequester: FocusRequester,
    snackBarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
) {
    val focusManager = LocalFocusManager.current
    BorderedTextField(
        value = state.email ?: "",
        padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        onValueChange = { updatedEmail -> onUpdateEmail(updatedEmail) },
        placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Go,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
            ),
        keyboardActions =
            KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    if (state.isCredentialValid) {
                        onSubmitInfo()
                    } else {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.email_validation_error),
                            )
                        }
                    }
                },
            ),
        onFocusChanged = { if (it.isFocused) onEnableAutofocus() },
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled,
    )
}

@Composable
private fun PhoneTextField(
    state: SignUpState,
    onUpdatePhone: (String) -> Unit,
    onSubmitInfo: () -> Unit,
    onEnableAutofocus: () -> Unit,
    focusRequester: FocusRequester,
    snackBarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
) {
    val focusManager = LocalFocusManager.current
    BorderedTextField(
        value = state.phone ?: "",
        padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        onValueChange = { updatedPhone -> onUpdatePhone(updatedPhone) },
        placeholder = { Text(text = stringResource(id = R.string.phone_placeholder), color = Color.White) },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Go,
            ),
        keyboardActions =
            KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    if (state.isCredentialValid) {
                        onSubmitInfo()
                    } else {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.phone_validation_error),
                            )
                        }
                    }
                },
            ),
        onFocusChanged = { if (it.isFocused) onEnableAutofocus() },
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled,
    )
}

@Composable
fun <T : Credential> CredentialButton(
    credential: Credential,
    credentialType: Class<T>,
    placeholderTextRes: Int,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        modifier =
            Modifier
                .padding(end = 12.dp)
                .size(120.dp, 50.dp),
        onClick = onClick,
        colors = AppButtonColors.ProgressGreen,
        isSelected = credentialType.isInstance(credential),
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
    state: SignUpState,
    onHandleAction: (SignupAction) -> Unit = {},
) {
    CustomDialog(
        title = stringResource(R.string.user_exists_header),
        textContent = stringResource(R.string.user_exists_message),
        onNegativeClick = { onHandleAction(SignupAction.CloseExistingUserDialog) },
        content = {
            state.existingUser?.let { user ->
                UserButton(
                    user = user,
                    isSelected = false,
                    buttonColors = AppButtonColors.Default,
                    selectedColor = Green,
                    onClick = { onHandleAction(SignupAction.ExistingUserSelected(user)) },
                )
            }
        },
    )
}