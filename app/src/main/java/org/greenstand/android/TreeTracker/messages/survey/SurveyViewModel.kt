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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class SurveyScreenState(
    val userImagePath: String? = null,
    val currentQuestion: Question? = null,
    val selectedAnswerIndex: Int? = null,
)

class SurveyViewModel(
    private val messageId: String,
    private val messagesRepo: MessagesRepo,
    private val userRepo: UserRepo,
) : ViewModel() {

    private var _state: MutableStateFlow<SurveyScreenState> = MutableStateFlow(SurveyScreenState())
    val state: StateFlow<SurveyScreenState> = _state.asStateFlow()

    private lateinit var survey: SurveyMessage
    private var currentQuestionIndex: Int = 0
    private val answers: Array<Int?> = Array(3) { null }

    init {
        viewModelScope.launch {
            survey = messagesRepo.getSurveyMessage(messageId)
            val user = userRepo.getUserWithWallet(survey.to)
            _state.value = _state.value.copy(
                userImagePath = user!!.photoPath,
                currentQuestion = survey.questions[currentQuestionIndex]
            )
            messagesRepo.markMessageAsRead(messageId)
        }
    }

    fun selectAnswer(answerIndex: Int) {
        answers[currentQuestionIndex] = answerIndex
        _state.value = _state.value.copy(
            selectedAnswerIndex = answers[currentQuestionIndex],
        )
    }

    /**
     * return false if there are no more questions
     * otherwise move to next question
     */
    suspend fun goToNextQuestion(): Boolean {
        if (currentQuestionIndex == survey.questions.size - 1) {
            val answerStrings = survey.questions.mapIndexed { index, question ->
                answers[index]?.let { question.choices[it] }
            }.requireNoNulls()
            messagesRepo.saveSurveyAnswers(messageId, answerStrings)
            return false
        }
        currentQuestionIndex++
        _state.value = _state.value.copy(
            selectedAnswerIndex = answers[currentQuestionIndex],
            currentQuestion = survey.questions[currentQuestionIndex]
        )
        return true
    }

    /**
     * return false if there are no previous questions
     * otherwise move to previous question
     */
    fun goToPrevQuestion(): Boolean {
        if (currentQuestionIndex == 0) {
            return false
        }
        currentQuestionIndex--
        _state.value = _state.value.copy(
            selectedAnswerIndex = answers[currentQuestionIndex],
            currentQuestion = survey.questions[currentQuestionIndex]
        )
        return true
    }
}

class SurveyViewModelFactory(private val messageId: String) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SurveyViewModel(messageId, get(), get()) as T
    }
}