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
package org.greenstand.android.TreeTracker.models.messages

interface Message {
    val id: String
    val from: String
    val to: String
    val composedAt: String
    val isRead: Boolean
}

data class DirectMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val parentMessageId: String?,
    val body: String,
) : Message

data class AnnouncementMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val subject: String,
    val body: String?,
    val videoLink: String?,
) : Message

data class SurveyMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val surveyId: String,
    val title: String,
    val questions: List<Question>,
    val isComplete: Boolean,
) : Message

data class SurveyResponseMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val surveyId: String,
    val questions: List<Question>,
    val responses: List<String>,
) : Message

data class Question(
    val prompt: String,
    val choices: List<String>,
)