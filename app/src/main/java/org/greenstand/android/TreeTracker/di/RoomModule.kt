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
package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.models.messages.database.MessageDatabase
import org.koin.dsl.module

val roomModule = module {

    single { AppDatabase.getInstance(get()) }

    single { AppDatabase.getInstance(get()).treeTrackerDao() }

    single { MessageDatabase.getInstance(get()) }

    single { MessageDatabase.getInstance(get()).messagesDao() }
}