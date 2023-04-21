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
package org.greenstand.android.TreeTracker.messages.survey

import android.widget.Toast
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.RoundedLocalImageContainer
import org.greenstand.android.TreeTracker.view.TreeTrackerButton

@Composable
fun SurveyScreen(
    messageId: String,
    viewModel: SurveyViewModel = viewModel(factory = SurveyViewModelFactory(messageId))
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState(SurveyScreenState())
    val scope = rememberCoroutineScope()
    var showToast by remember { mutableStateOf(false) }
    if (showToast) {
        ShowToastMessage(stringResId = R.string.survey_completed)
    }
    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    UserImage(state.userImagePath)
                },
            )
        },
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = AppButtonColors.MessagePurple,
                    ) {
                        if (!viewModel.goToPrevQuestion()) {
                            navController.popBackStack()
                        }
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedAnswerIndex != null,
                        colors = AppButtonColors.MessagePurple,
                        onClick = {
                            scope.launch {
                                if (!viewModel.goToNextQuestion()) {
                                    showToast = true
                                    navController.popBackStack()
                                }
                            }
                        }
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuestionPrompt(promptText = state.currentQuestion?.prompt ?: "")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                state.currentQuestion?.choices?.let { choices ->
                    items(
                        count = choices.size,
                        key = { it }
                    ) { index ->
                        AnswerItem(
                            answerText = choices[index],
                            isSelected = state.selectedAnswerIndex == index,
                            onClick = {
                                viewModel.selectAnswer(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.UserImage(imagePath: String?) {
    imagePath?.let {
        RoundedLocalImageContainer(
            imagePath = it,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun QuestionPrompt(promptText: String) {
    Text(
        modifier = Modifier.padding(12.dp),
        text = promptText,
        textAlign = TextAlign.Center,
        color = CustomTheme.textColors.lightText,
        style = CustomTheme.typography.large
    )
}

@Composable
fun AnswerItem(
    answerText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        colors = if (isSelected) AppButtonColors.ProgressGreen else AppButtonColors.Default,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                text = answerText,
                textAlign = TextAlign.Center,
                color = CustomTheme.textColors.lightText,
                style = CustomTheme.typography.large
            )
        }
    }
}
@Composable
fun ShowToastMessage(stringResId: Int) {
    val context = LocalContext.current
    val message = stringResource(id = stringResId)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}