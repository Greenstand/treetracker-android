package org.greenstand.android.TreeTracker.messages.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class SurveyScreenState(
    val userImagePath: String? = null,
    val currentQuestion: Question? = null,
    val selectedAnswer: String = "",
)

class SurveyViewModel(
    private val messageId: String,
    private val messagesRepo: MessagesRepo,
    private val users: Users,
) : ViewModel() {

    private var _state: MutableStateFlow<SurveyScreenState> = MutableStateFlow( SurveyScreenState())
    val state: StateFlow<SurveyScreenState> = _state.asStateFlow()

    private lateinit var survey: SurveyMessage
    private var currentQuestionIndex: Int = 0
    private val answers: Array<String> = Array(3) { "" }

    init {
        viewModelScope.launch {
            survey = messagesRepo.getSurveyMessage(messageId)
            val user = users.getUserWithWallet(survey.to)
            _state.value = _state.value.copy(
                userImagePath = user!!.photoPath,
                currentQuestion = survey.questions[currentQuestionIndex]
            )
        }
    }

    fun selectAnswer(answer: String) {
        answers[currentQuestionIndex] = answer
        _state.value = _state.value.copy(
            selectedAnswer = answers[currentQuestionIndex],
        )
    }

    /**
     * return false if there are no more questions
     * otherwise move to next question
     */
    suspend fun goToNextQuestion(): Boolean {
        if (currentQuestionIndex == survey.questions.size - 1) {
            messagesRepo.saveSurveyAnswers(messageId, answers.toList())
            return false
        }
        currentQuestionIndex++
        _state.value = _state.value.copy(
            selectedAnswer = answers[currentQuestionIndex],
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
            selectedAnswer = answers[currentQuestionIndex],
            currentQuestion = survey.questions[currentQuestionIndex]
        )
        return true
    }

}

class SurveyViewModelFactory(private val messageId: String)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SurveyViewModel(messageId, get(), get()) as T
    }
}