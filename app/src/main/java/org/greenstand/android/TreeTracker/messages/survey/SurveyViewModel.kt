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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class SurveyScreenState(
    val userImagePath: String? = null,
    val currentQuestion: Question? = null,
    val selectedAnswerIndex: Int? = null,
    val surveyComplete: Boolean = false,
    val shouldNavigateBack: Boolean = false,
)

sealed class SurveyAction : Action {
    data class SelectAnswer(val answerIndex: Int) : SurveyAction()
    object GoToNextQuestion : SurveyAction()
    object GoToPrevQuestion : SurveyAction()
    object NavigateBack : SurveyAction()
}

class SurveyViewModel(
    private val messageId: String,
    private val messagesRepo: MessagesRepo,
    private val userRepo: UserRepo,
) : BaseViewModel<SurveyScreenState, SurveyAction>(SurveyScreenState()) {

    private lateinit var survey: SurveyMessage
    private var currentQuestionIndex: Int = 0
    private val answers: Array<Int?> = Array(3) { null }

    init {
        viewModelScope.launch {
            survey = messagesRepo.getSurveyMessage(messageId)
            val user = userRepo.getUserWithWallet(survey.to)
            updateState {
                copy(
                    userImagePath = user!!.photoPath,
                    currentQuestion = survey.questions[currentQuestionIndex]
                )
            }
            messagesRepo.markMessageAsRead(messageId)
        }
    }

    override fun handleAction(action: SurveyAction) {
        when (action) {
            is SurveyAction.SelectAnswer -> {
                answers[currentQuestionIndex] = action.answerIndex
                updateState { copy(selectedAnswerIndex = answers[currentQuestionIndex]) }
            }
            is SurveyAction.GoToNextQuestion -> goToNextQuestion()
            is SurveyAction.GoToPrevQuestion -> goToPrevQuestion()
            else -> { }
        }
    }

    private fun goToNextQuestion() {
        viewModelScope.launch {
            if (currentQuestionIndex == survey.questions.size - 1) {
                val answerStrings = survey.questions.mapIndexed { index, question ->
                    answers[index]?.let { question.choices[it] }
                }.requireNoNulls()
                messagesRepo.saveSurveyAnswers(messageId, answerStrings)
                updateState { copy(surveyComplete = true) }
                return@launch
            }
            currentQuestionIndex++
            updateState {
                copy(
                    selectedAnswerIndex = answers[currentQuestionIndex],
                    currentQuestion = survey.questions[currentQuestionIndex]
                )
            }
        }
    }

    private fun goToPrevQuestion() {
        if (currentQuestionIndex == 0) {
            updateState { copy(shouldNavigateBack = true) }
            return
        }
        currentQuestionIndex--
        updateState {
            copy(
                selectedAnswerIndex = answers[currentQuestionIndex],
                currentQuestion = survey.questions[currentQuestionIndex]
            )
        }
    }
}

class SurveyViewModelFactory(private val messageId: String) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SurveyViewModel(messageId, get(), get()) as T
    }
}
