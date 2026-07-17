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
import org.greenstand.android.TreeTracker.view.SharedKeyboardOptions
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
            CredentialBottomBar(
                state = state,
                snackBarHostState = snackBarHostState,
                scope = scope,
                context = context,
                onHandleAction = onHandleAction,
            )
        },
    ) {
        if (state.existingUser != null) {
            ExistingUserDialog(state = state, onHandleAction = onHandleAction)
        }

        CredentialFormContent(
            state = state,
            focusRequester = focusRequester,
            snackBarHostState = snackBarHostState,
            scope = scope,
            context = context,
            onHandleAction = onHandleAction,
        )

        if (state.showPrivacyDialog == true) {
            PrivacyPolicyDialog(onDismiss = { onHandleAction(SignupAction.ClosePrivacyPolicyDialog) })
        }
    }
}

@Composable
private fun CredentialBottomBar(
    state: SignUpState,
    snackBarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onHandleAction: (SignupAction) -> Unit,
) {
    if (state.showPrivacyDialog == true) return

    ActionBar(
        modifier = Modifier.navigationBarsPadding(),
        leftAction = {
            ArrowButton(isLeft = true) {
                onHandleAction(SignupAction.NavigateBack)
            }
        },
        rightAction = {
            ArrowButton(isLeft = false) {
                if (state.isCredentialValid) {
                    onHandleAction(SignupAction.SubmitInfo)
                } else {
                    val errorRes =
                        when (state.credential) {
                            is Credential.Email -> R.string.email_validation_error
                            is Credential.Phone -> R.string.phone_validation_error
                        }
                    scope.launch { snackBarHostState.showSnackbar(context.getString(errorRes)) }
                }
            }
        },
    )
}

@Composable
private fun CredentialFormContent(
    state: SignUpState,
    focusRequester: FocusRequester,
    snackBarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onHandleAction: (SignupAction) -> Unit,
) {
    val navigateToWebPage: () -> Unit = {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(Constants.TREE_TRACKER_URL)
            }
        startActivity(context, intent, null)
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
        CustomSnackbar(snackbarHostState = snackBarHostState, backGroundColor = AppColors.Red)

        CredentialTypeSelector(
            state = state,
            onUpdateCredentialType = { onHandleAction(SignupAction.UpdateCredentialType(it)) },
        )

        CredentialTextField(
            state = state,
            focusRequester = focusRequester,
            snackBarHostState = snackBarHostState,
            scope = scope,
            context = context,
            onHandleAction = onHandleAction,
        )

        ViewWebMapText(isVisible = state.isInternetAvailable, onClick = navigateToWebPage)
    }
}

@Composable
private fun CredentialTypeSelector(
    state: SignUpState,
    onUpdateCredentialType: (Credential) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        PhoneCredentialButton(state) { onUpdateCredentialType(it) }
        EmailCredentialButton(state) { onUpdateCredentialType(it) }
    }
}

@Composable
private fun CredentialTextField(
    state: SignUpState,
    focusRequester: FocusRequester,
    snackBarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onHandleAction: (SignupAction) -> Unit,
) {
    when (state.credential) {
        is Credential.Email -> {
            EmailTextField(
                state = state,
                onUpdateEmail = { onHandleAction(SignupAction.UpdateEmail(it)) },
                onSubmitInfo = { onHandleAction(SignupAction.SubmitInfo) },
                onEnableAutofocus = { onHandleAction(SignupAction.EnableAutofocus) },
                focusRequester = focusRequester,
                snackBarHostState = snackBarHostState,
                scope = scope,
                context = context,
            )
        }

        is Credential.Phone -> {
            PhoneTextField(
                state = state,
                onUpdatePhone = { onHandleAction(SignupAction.UpdatePhone(it)) },
                onSubmitInfo = { onHandleAction(SignupAction.SubmitInfo) },
                onEnableAutofocus = { onHandleAction(SignupAction.EnableAutofocus) },
                focusRequester = focusRequester,
                snackBarHostState = snackBarHostState,
                scope = scope,
                context = context,
            )
        }
    }
}

@Composable
private fun CredentialBorderedTextField(
    value: String,
    placeholderRes: Int,
    keyboardType: KeyboardType,
    capitalization: KeyboardCapitalization,
    autoCorrect: Boolean,
    focusRequester: FocusRequester,
    autofocusEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    onImeGo: () -> Unit,
) {
    BorderedTextField(
        value = value,
        padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        onValueChange = onValueChange,
        placeholder = { Text(text = stringResource(id = placeholderRes), color = Color.White) },
        keyboardOptions =
            SharedKeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                capitalization = capitalization,
                autoCorrectEnabled = autoCorrect,
            ),
        keyboardActions = KeyboardActions(onGo = { onImeGo() }),
        onFocusChanged = onFocusChanged,
        focusRequester = focusRequester,
        autofocusEnabled = autofocusEnabled,
    )
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
    CredentialBorderedTextField(
        value = state.email ?: "",
        placeholderRes = R.string.email_placeholder,
        keyboardType = KeyboardType.Email,
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false,
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled,
        onValueChange = onUpdateEmail,
        onFocusChanged = { if (it.isFocused) onEnableAutofocus() },
        onImeGo = {
            focusManager.clearFocus()
            if (state.isCredentialValid) {
                onSubmitInfo()
            } else {
                scope.launch {
                    snackBarHostState.showSnackbar(context.getString(R.string.email_validation_error))
                }
            }
        },
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
    CredentialBorderedTextField(
        value = state.phone ?: "",
        placeholderRes = R.string.phone_placeholder,
        keyboardType = KeyboardType.Phone,
        capitalization = KeyboardCapitalization.None,
        autoCorrect = true,
        focusRequester = focusRequester,
        autofocusEnabled = state.autofocusTextEnabled,
        onValueChange = onUpdatePhone,
        onFocusChanged = { if (it.isFocused) onEnableAutofocus() },
        onImeGo = {
            focusManager.clearFocus()
            if (state.isCredentialValid) {
                onSubmitInfo()
            } else {
                scope.launch {
                    snackBarHostState.showSnackbar(context.getString(R.string.phone_validation_error))
                }
            }
        },
    )
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
        onClick = { onUpdateCredentialType(Credential.Email()) },
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
        onClick = { onUpdateCredentialType(Credential.Phone()) },
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
            modifier =
                Modifier
                    .padding(top = 10.dp)
                    .clickable(onClick = onClick),
            color = Color.White,
            style = TextStyle(textDecoration = TextDecoration.Underline),
        )
    }
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