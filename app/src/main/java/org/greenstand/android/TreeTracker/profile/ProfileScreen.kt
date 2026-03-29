package org.greenstand.android.TreeTracker.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.userselect.UserSelectAction
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModelFactory
import org.greenstand.android.TreeTracker.utils.ValidationUtils
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.ProfileField
import org.greenstand.android.TreeTracker.view.TreeTrackerButton

@Composable
fun ProfileScreen(
    userId: Long,
) {
    val viewModel: UserSelectViewModel = viewModel(factory = UserSelectViewModelFactory(userId = userId))
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { newPhotoPath ->
        if (!newPhotoPath.isNullOrEmpty()) {
            state.selectedUser?.photoPath = newPhotoPath
            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(photoPath = newPhotoPath))
        }
    }

    Profile(
        state = state,
        onHandleAction = { action ->
            when (action) {
                is UserSelectAction.NavigateBack -> navController.popBackStack()
                is UserSelectAction.NavigateToPhoto -> cameraLauncher.launch(true)
                is UserSelectAction.SaveUserToDatabase -> {
                    viewModel.handleAction(action)
                    viewModel.handleAction(UserSelectAction.ToggleEditMode)
                }
                else -> viewModel.handleAction(action)
            }
        },
    )
}

@Composable
fun Profile(
    state: UserSelectState = UserSelectState(),
    onHandleAction: (UserSelectAction) -> Unit = {},
) {
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    Text(
                        text = stringResource(id = R.string.profile_title),
                        color = AppColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = CustomTheme.typography.large,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                leftAction = {
                    ArrowButton(isLeft = true) {
                        onHandleAction(UserSelectAction.NavigateBack)
                    }
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val selectedUser = state.selectedUser

            if (selectedUser != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 20.dp, end = 20.dp),
                ) {
                    LocalImage(
                        imagePath = selectedUser.photoPath,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .aspectRatio(1.0f)
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = state.editMode) { onHandleAction(UserSelectAction.NavigateToPhoto) },
                    )
                }

                TreeTrackerButton(
                    onClick = { onHandleAction(UserSelectAction.ToggleEditMode) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                        .size(height = 80.dp, width = 156.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = if (state.editMode) stringResource(id = R.string.cancel_edit) else stringResource(id = R.string.edit_profile),
                        fontWeight = FontWeight.Bold,
                        color = CustomTheme.textColors.primaryText,
                        style = CustomTheme.typography.regular,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProfileField(stringResource(id = R.string.first_name_hint), selectedUser.firstName, state.editMode) { newFirstName ->
                    val filtered = ValidationUtils.filterNameInput(newFirstName)
                    onHandleAction(UserSelectAction.UpdateSelectedUser(firstName = filtered))
                    if (state.editMode) {
                        val (_, error) = ValidationUtils.validateName(filtered)
                        firstNameError = error
                    }
                }

                if (state.editMode && firstNameError != null) {
                    Text(
                        text = firstNameError!!,
                        color = MaterialTheme.colors.error,
                        style = CustomTheme.typography.small,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                ProfileField(
                    stringResource(id = R.string.last_name_hint), selectedUser.lastName ?: "", state.editMode
                ) { newLastName ->
                    val filtered = ValidationUtils.filterNameInput(newLastName)
                    onHandleAction(UserSelectAction.UpdateSelectedUser(lastName = filtered))
                    if (state.editMode) {
                        val (_, error) = ValidationUtils.validateName(filtered)
                        lastNameError = error
                    }
                }

                if (state.editMode && lastNameError != null) {
                    Text(
                        text = lastNameError!!,
                        color = MaterialTheme.colors.error,
                        style = CustomTheme.typography.small,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                if (selectedUser.wallet.contains("@")) {
                    ProfileField(stringResource(id = R.string.email_placeholder), selectedUser.wallet, state.editMode) {
                        onHandleAction(UserSelectAction.UpdateSelectedUser(email = it))
                    }
                } else {
                    ProfileField(stringResource(id = R.string.phone_placeholder), selectedUser.wallet, state.editMode) {
                        onHandleAction(UserSelectAction.UpdateSelectedUser(phone = it))
                    }
                }

                if (state.editMode) {
                    Spacer(modifier = Modifier.height(24.dp))

                    TreeTrackerButton(
                        colors = AppButtonColors.ProgressGreen,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 150.dp, 60.dp),
                        onClick = {
                            val firstName = state.selectedUser?.firstName ?: ""
                            val lastName = state.selectedUser?.lastName ?: ""

                            if (ValidationUtils.validateName(firstName).first &&
                                ValidationUtils.validateName(lastName).first
                            ) {
                                onHandleAction(UserSelectAction.SaveUserToDatabase)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.save_changes),
                            fontWeight = FontWeight.Bold,
                            color = CustomTheme.textColors.darkText,
                            style = CustomTheme.typography.regular
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(id = R.string.loading_user_profile),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
