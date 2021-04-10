package org.greenstand.android.TreeTracker.signup

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.ui.colorPrimary
import org.greenstand.android.TreeTracker.ui.mediumGrey
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.TreeTrackerTextButton

@Composable
fun SignupFlow(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val uiState by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    // TODO: Handle the back behavior! Send the user back to the email, name entry screen!

    Scaffold( // TODO: This scaffold should be moved to a host view so we don't have to repeat code
        topBar = {
            ActionBar(
                centerAction = { Text("Treetracker", color = colorPrimary) },
                rightAction = {
                    TreeTrackerTextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.language,
                        onClick = { navController.navigate(NavRoute.Language.create(isFromTopBar = true)) }
                    )
                }
            )
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // TODO disable button until input fields are valid
                Button(
                    onClick = {
                        navController.navigate(NavRoute.NameEntryView.route)
                    }
                ) {
                    Text(stringResource(id = R.string.next))
                }
            }
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

                Button(
                    modifier = Modifier.padding(end = 4.dp),
                    onClick = {
                        viewModel.updateCredentialType(CredentialType.Email)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (uiState.credentialType == CredentialType.Email) {
                            MaterialTheme.colors.primary
                        } else {
                            mediumGrey
                        }
                    )
                ) {
                    Text(
                        text = "Email",
                        color = Color.Black
                    )
                }

                Button(
                    modifier = Modifier.padding(start = 4.dp),
                    onClick = {
                        viewModel.updateCredentialType(CredentialType.Phone)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (uiState.credentialType == CredentialType.Phone) {
                            MaterialTheme.colors.primary
                        } else {
                            mediumGrey
                        }
                    )
                ) {
                    Text(
                        text = "Phone",
                        color = Color.Black
                    )
                }
            }

            if (uiState.showEmailText) {
                BorderedTextField(
                    value = uiState.emailText,
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
                    placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) }
                )
            }

            if (uiState.showPhoneText) {
                BorderedTextField(
                    value = uiState.phoneText,
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedPhone -> viewModel.updatePhone(updatedPhone) },
                    placeholder = { Text(text = stringResource(id = R.string.phone_placeholder), color = Color.White) }
                )
            }
        }
    }
}

@Preview
@Composable
fun SignupScreen_Preview(@PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel) {
    SignupFlow(viewModel = viewModel)
}
