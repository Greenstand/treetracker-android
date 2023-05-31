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
package org.greenstand.android.TreeTracker.database.messages

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.greenstand.android.TreeTracker.database.app.Converters
import org.greenstand.android.TreeTracker.database.messages.dao.MessagesDAO
import org.greenstand.android.TreeTracker.database.messages.entities.MessageEntity
import org.greenstand.android.TreeTracker.database.messages.entities.QuestionEntity
import org.greenstand.android.TreeTracker.database.messages.entities.SurveyEntity

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        MessageEntity::class,
        SurveyEntity::class,
        QuestionEntity::class,
    ],
)
@TypeConverters(Converters::class)
abstract class MessageDatabase : RoomDatabase() {

    abstract fun messagesDao(): MessagesDAO

    companion object {

        const val DB_NAME = "treetracker.messages.db"
        const val DB_NAME_ENCRYPT = "treetracker.messages-encrypt.db"


    }
}