package org.greenstand.android.TreeTracker.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.*
import org.greenstand.android.TreeTracker.view.AppColors.Green
import org.greenstand.android.TreeTracker.view.AppColors.MediumGray

@Composable
fun SignupFlow(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current

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
                    ArrowButton(isLeft = false) {
                        navController.navigate(NavRoute.NameEntryView.route)
                    }
                }
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                DepthButton(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(62.dp, 40.dp),
                    onClick = {
                        viewModel.updateCredentialType(Credential.Email())
                    },
                    colors = DepthButtonColors(
                        color = if (state.credential is Credential.Email) {
                            Green
                        } else {
                            MediumGray
                        },
                        shadowColor = AppColors.GrayShadow,
                        disabledColor = AppColors.GrayShadow,
                        disabledShadowColor = AppColors.GrayShadow
                    ),
                    isSelected = state.credential is Credential.Email
                ) {
                    Text(
                        text = stringResource(id = R.string.email_placeholder),
                        color = Color.Black,
                    )
                }

                DepthButton(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(62.dp, 40.dp),
                    onClick = {
                        viewModel.updateCredentialType(Credential.Phone())
                    },
                    colors = DepthButtonColors(
                        color = if (state.credential is Credential.Phone) {
                            Green
                        } else {
                            MediumGray
                        },
                        shadowColor = AppColors.GrayShadow,
                        disabledColor = AppColors.GrayShadow,
                        disabledShadowColor = AppColors.GrayShadow
                    ),
                    isSelected = state.credential is Credential.Phone
                ) {
                    Text(
                        text = stringResource(id = R.string.phone_placeholder),
                        color = Color.Black,
                    )
                }
            }

            when (val credential = state.credential) {
                is Credential.Email -> BorderedTextField(
                    value = credential.text,
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
                    placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                is Credential.Phone -> BorderedTextField(
                    value = credential.text,
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedPhone -> viewModel.updatePhone(updatedPhone) },
                    placeholder = { Text(text = stringResource(id = R.string.phone_placeholder), color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }
    }
}

@Preview
@Composable
fun SignupScreen_Preview(
    @PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel
) {
    SignupFlow(viewModel = viewModel)
}
