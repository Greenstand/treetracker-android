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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

@Composable
fun SignUpScreen(viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val state by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { photoPath ->
        scope.launch {
            viewModel.createUser(photoPath)?.let { user ->
                if (user.isPowerUser) {
                    navController.navigate(DashboardRoute) {
                        popUpTo<LanguageRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    CaptureSetupScopeManager.getData().user = user
                    scope.launch { CaptureSetupScopeManager.nav.navFromNewUserCreation(navController) }
                }
            }
        }
    }

    SignUp(
        state = state,
        onBackClicked = { navController.popBackStack() },
        onSubmitInfo = { viewModel.submitInfo() },
        onUpdateEmail = { viewModel.updateEmail(it) },
        onUpdatePhone = { viewModel.updatePhone(it) },
        onUpdateCredentialType = { viewModel.updateCredentialType(it) },
        onCloseExistingUserDialog = { viewModel.closeExistingUserDialog() },
        onExistingUserSelected = { user ->
            if (state.isTherePowerUser == false) {
                viewModel.setExistingUserAsPowerUser(user.id)
                navController.navigate(DashboardRoute) {
                    popUpTo<LanguageRoute> { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                CaptureSetupScopeManager.getData().user = user
                scope.launch { CaptureSetupScopeManager.nav.navFromNewUserCreation(navController) }
            }
        },
        onEnableAutofocus = { viewModel.enableAutofocus() },
        onClosePrivacyDialog = { viewModel.closePrivacyPolicyDialog() },
        onUpdateFirstName = { viewModel.updateFirstName(it) },
        onUpdateLastName = { viewModel.updateLastName(it) },
        isFormValid = { viewModel.isFormValid() },
        onGoToCredentialEntry = { viewModel.goToCredentialEntry() },
        onLaunchCamera = { cameraLauncher.launch(true) },
    )
}

@Composable
fun SignUp(
    state: SignUpState = SignUpState(),
    onBackClicked: () -> Unit = {},
    onSubmitInfo: () -> Unit = {},
    onUpdateEmail: (String) -> Unit = {},
    onUpdatePhone: (String) -> Unit = {},
    onUpdateCredentialType: (Credential) -> Unit = {},
    onCloseExistingUserDialog: () -> Unit = {},
    onExistingUserSelected: (User) -> Unit = {},
    onEnableAutofocus: () -> Unit = {},
    onClosePrivacyDialog: () -> Unit = {},
    onUpdateFirstName: (String?) -> Unit = {},
    onUpdateLastName: (String?) -> Unit = {},
    isFormValid: () -> Boolean = { false },
    onGoToCredentialEntry: () -> Unit = {},
    onLaunchCamera: () -> Unit = {},
) {
    if (state.isCredentialView) {
        CredentialEntryView(
            state = state,
            onBackClicked = onBackClicked,
            onSubmitInfo = onSubmitInfo,
            onUpdateEmail = onUpdateEmail,
            onUpdatePhone = onUpdatePhone,
            onUpdateCredentialType = onUpdateCredentialType,
            onCloseExistingUserDialog = onCloseExistingUserDialog,
            onExistingUserSelected = onExistingUserSelected,
            onEnableAutofocus = onEnableAutofocus,
            onClosePrivacyDialog = onClosePrivacyDialog,
        )
    } else {
        NameEntryView(
            state = state,
            onUpdateFirstName = onUpdateFirstName,
            onUpdateLastName = onUpdateLastName,
            isFormValid = isFormValid,
            onGoToCredentialEntry = onGoToCredentialEntry,
            onLaunchCamera = onLaunchCamera,
        )
    }
}
