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