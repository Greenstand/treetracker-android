package org.greenstand.android.TreeTracker.messages.survey

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.RoundedLocalImageContainer

@Composable
fun SurveyScreen(
    messageId: String,
    viewModel: SurveyViewModel = viewModel(factory = SurveyViewModelFactory(messageId))
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState(SurveyScreenState())
    val scope = rememberCoroutineScope()

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
                        isEnabled = state.selectedAnswer.isNotBlank(),
                        colors = AppButtonColors.MessagePurple,
                    ) {
                        scope.launch {
                            if (!viewModel.goToNextQuestion()) {
                                navController.popBackStack()
                            }
                        }
                    }
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
                        items = choices,
                        key = { it }
                    ) { choice ->
                        AnswerItem(
                            answerText = choice,
                            isSelected = state.selectedAnswer == choice,
                            onClick = {
                                viewModel.selectAnswer(choice)
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
    DepthButton(
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