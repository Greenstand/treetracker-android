package org.greenstand.android.TreeTracker.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.settings.SettingsItem
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.ProfileField
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import java.io.File

@Composable
fun ProfileScreen(
    userId: Long,
) {
    val viewModel: UserSelectViewModel = viewModel(factory = UserSelectViewModelFactory(userId = userId))
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState(UserSelectState())

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { newPhotoPath ->
        if (!newPhotoPath.isNullOrEmpty()) {
            state.selectedUser?.photoPath = newPhotoPath
            scope.launch {
                viewModel.updateSelectedUser(photoPath = newPhotoPath)
            }
        }
    }
    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    Text(
                        text = stringResource(id = R.string.settings),
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
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
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
                            .clickable(enabled = state.editMode) { cameraLauncher.launch(true) },
                    )
                }

                TreeTrackerButton(
                    onClick = { viewModel.updateEditEnabled() },
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

                ProfileField(stringResource(id = R.string.first_name_hint), selectedUser.firstName, state.editMode) {
                    viewModel.updateSelectedUser(firstName = it)
                }

                ProfileField(stringResource(id = R.string.last_name_hint), selectedUser.lastName ?: "", state.editMode) {
                    viewModel.updateSelectedUser(lastName = it)
                }
                if (selectedUser.wallet.contains("@")) {
                    ProfileField(stringResource(id = R.string.email_placeholder), selectedUser.wallet ?: "", state.editMode) {
                        viewModel.updateSelectedUser(email = it)
                    }
                } else {
                    ProfileField(stringResource(id = R.string.phone_placeholder), selectedUser.wallet ?: "", state.editMode) {
                        viewModel.updateSelectedUser(phone = it)
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

                            scope.launch {
                                viewModel.updateUserInDatabase()
                                viewModel.updateEditEnabled()
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

