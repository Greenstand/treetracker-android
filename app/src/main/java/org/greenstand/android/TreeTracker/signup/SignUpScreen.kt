package org.greenstand.android.TreeTracker.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

@Composable
fun SignUpScreen(viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)) {
   val state by viewModel.state.observeAsState(SignUpState())

   if(state.nameEntryStage) {
      NameEntryView(viewModel)
   } else {
      CredentialEntryView(viewModel)
   }
}
