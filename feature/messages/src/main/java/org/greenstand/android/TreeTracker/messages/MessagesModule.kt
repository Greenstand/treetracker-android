/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.messages

import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementScreen
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListScreen
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreen
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import androidx.compose.foundation.ExperimentalFoundationApi
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Messages: message lists, chat, announcements and surveys.
@OptIn(KoinExperimentalAPI::class, ExperimentalFoundationApi::class)
val messagesModule =
    module {
        viewModel { IndividualMessageListViewModel(get(), get(), get()) }
        viewModel { ChatViewModel(get(), get(), get(), get()) }
        viewModel { AnnouncementViewModel(get(), get()) }

        navigation<MessagesUserSelectRoute> { _ -> MessagesUserSelectScreen() }
        navigation<IndividualMessageListRoute> { route -> IndividualMessageListScreen(route.planterInfoId) }
        navigation<SurveyRoute> { route -> SurveyScreen(route.messageId) }
        navigation<ChatRoute> { route -> ChatScreen(route.planterInfoId, route.otherChatIdentifier) }
        navigation<AnnouncementRoute> { route -> AnnouncementScreen(route.messageId) }
    }