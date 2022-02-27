package org.greenstand.android.TreeTracker.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import org.koin.androidx.compose.getKoin
import org.koin.androidx.compose.getViewModel
import org.koin.core.qualifier.named

@Composable
fun SignUpScreen() {
   val scope = getKoin().getOrCreateScope("SIGN_UP_SCOPE", named("SIGN_UP"))
   val viewModel = getViewModel<SignupViewModel>(
      owner = getComposeViewModelOwner(),
      scope = scope
   )
   val state by viewModel.state.observeAsState(SignUpState())

   if (state.isCredentialView) {
      CredentialEntryView(viewModel, state)
   } else {
      NameEntryView(viewModel, state)
   }
}
