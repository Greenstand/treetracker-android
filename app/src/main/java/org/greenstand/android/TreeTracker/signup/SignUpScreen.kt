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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

@Composable
fun SignUpScreen(viewModel: SignupViewModel = viewModel(factory = LocalViewModelFactory.current)) {
   val state by viewModel.state.observeAsState(SignUpState())

   if (state.isCredentialView) {
      CredentialEntryView(viewModel, state)
   } else {
      NameEntryView(viewModel, state)
   }
}
