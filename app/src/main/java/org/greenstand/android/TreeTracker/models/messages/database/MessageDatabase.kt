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
package org.greenstand.android.TreeTracker.models.messages.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.greenstand.android.TreeTracker.database.Converters
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity

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

        private var INSTANCE: MessageDatabase? = null

        fun getInstance(context: Context): MessageDatabase {
            if (INSTANCE == null) {
                synchronized(MessageDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MessageDatabase::class.java,
                        DB_NAME
                    ).build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME = "treetracker.messages.db"
    }
}