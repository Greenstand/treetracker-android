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
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppColors.MediumGray
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.LanguageButton

@Composable
fun SignupFlow(
    viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.state.observeAsState(SignUpState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    // TODO: Handle the back behavior! Send the user back to the email, name entry screen!

    Scaffold( // TODO: This scaffold should be moved to a host view so we don't have to repeat code
        topBar = {
            ActionBar(
                centerAction = { Text(stringResource(id = R.string.treetracker)) },
                rightAction = {
                    LanguageButton()
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
                        viewModel.updateCredentialType(Credential.Email)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (state.credential is Credential.Email) {
                            MaterialTheme.colors.primary
                        } else {
                            MediumGray
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
                        viewModel.updateCredentialType(Credential.Phone)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (state.credential is Credential.Phone) {
                            MaterialTheme.colors.primary
                        } else {
                            MediumGray
                        }
                    )
                ) {
                    Text(
                        text = "Phone",
                        color = Color.Black
                    )
                }
            }

            when (state.credential) {
                Credential.Email -> BorderedTextField(
                    value = state.credential.text,
                    padding = PaddingValues(16.dp),
                    onValueChange = { updatedEmail -> viewModel.updateEmail(updatedEmail) },
                    placeholder = { Text(text = stringResource(id = R.string.email_placeholder), color = Color.White) }
                )

                Credential.Phone -> BorderedTextField(
                    value = state.credential.text,
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
fun SignupScreen_Preview(
    @PreviewParameter(SignupViewPreviewProvider::class) viewModel: SignupViewModel
) {
    SignupFlow(viewModel = viewModel)
}
