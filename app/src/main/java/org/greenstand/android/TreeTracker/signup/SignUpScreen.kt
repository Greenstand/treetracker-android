package org.greenstand.android.TreeTracker.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.koin.core.context.GlobalContext.get

@Composable
fun SignUpScreen(viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)) {
   val state by viewModel.credentialState.observeAsState(CredentialState())

   if(state.nameEntryStage) {
      NameEntryView(viewModel)
   }
   else {
      CredentialEntryView(viewModel)
   }
}
