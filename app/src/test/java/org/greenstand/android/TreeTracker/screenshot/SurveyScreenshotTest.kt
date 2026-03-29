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
package org.greenstand.android.TreeTracker.screenshot

import org.greenstand.android.TreeTracker.messages.survey.Survey
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreenState
import org.greenstand.android.TreeTracker.models.messages.Question
import org.junit.Test

class SurveyScreenshotTest : ScreenshotTest() {
    @Test
    fun survey_default() =
        snapshot {
            Survey(state = SurveyScreenState())
        }

    @Test
    fun survey_with_question() =
        snapshot {
            Survey(
                state =
                    SurveyScreenState(
                        currentQuestion =
                            Question(
                                prompt = "How healthy are the trees you planted?",
                                choices = listOf("Very healthy", "Somewhat healthy", "Not healthy", "Dead"),
                            ),
                    ),
            )
        }

    @Test
    fun survey_with_selected_answer() =
        snapshot {
            Survey(
                state =
                    SurveyScreenState(
                        currentQuestion =
                            Question(
                                prompt = "What type of soil is at the planting site?",
                                choices = listOf("Sandy", "Clay", "Loam", "Rocky"),
                            ),
                        selectedAnswerIndex = 2,
                    ),
            )
        }
}