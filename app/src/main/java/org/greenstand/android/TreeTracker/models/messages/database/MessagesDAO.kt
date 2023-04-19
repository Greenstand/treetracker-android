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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity

@Dao
interface MessagesDAO {

    /**
     * Messages
     */

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(messageEntity: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessage(id: String): MessageEntity?

    @Query("SELECT MAX(composed_at) FROM messages WHERE wallet = :wallet")
    suspend fun getLatestSyncTimeForWallet(wallet: String): String?

    @Query("SELECT * FROM messages WHERE wallet = :wallet")
    suspend fun getMessagesForWallet(wallet: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE wallet = :wallet")
    fun getMessagesForWalletFlow(wallet: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE wallet = :wallet AND type = 'MESSAGE'")
    fun getDirectMessagesForWallet(wallet: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE wallet = :wallet AND type = 'ANNOUNCE'")
    fun getAnnouncementMessagesForWallet(wallet: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET bundle_id = :bundleId WHERE id IN (:ids)")
    suspend fun updateMessageBundleIds(ids: List<String>, bundleId: String)

    @Query("SELECT * FROM messages WHERE should_upload = 1")
    suspend fun getMessagesToUpload(): List<MessageEntity>

    @Query("SELECT id FROM messages WHERE should_upload = 1")
    suspend fun getMessageIdsToUpload(): List<String>

    @Query("SELECT * FROM messages WHERE id IN (:ids)")
    suspend fun getMessagesByIds(ids: List<String>): List<MessageEntity>

    @Query("UPDATE messages SET should_upload = 0 WHERE id IN (:ids)")
    suspend fun markMessagesAsUploaded(ids: List<String>)

    @Query("UPDATE messages SET is_read = 1 WHERE id IN (:id)")
    suspend fun markMessageAsRead(id: List<String>)

    @Query("UPDATE messages SET is_survey_complete = 1 WHERE id = :id")
    suspend fun markSurveyMessageComplete(id: String?)

    @Query("SELECT COUNT(*) FROM messages WHERE is_read = 0")
    suspend fun getUnreadMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE wallet = (:wallet) AND is_read = 0")
    suspend fun getUnreadMessageCountForWallet(wallet: String): Int

    /**
     * Surveys
     */

    @Query("DELETE FROM surveys WHERE id = :id")
    suspend fun deleteSurvey(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSurvey(surveyEntity: SurveyEntity): Long

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getSurvey(id: String?): SurveyEntity?

    /**
     * Questions
     */

    @Query("DELETE FROM questions WHERE _id IN (:ids)")
    suspend fun deleteQuestions(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestion(questionEntity: QuestionEntity): Long

    @Query("SELECT * FROM questions WHERE survey_id = :surveyId")
    suspend fun getQuestionsForSurvey(surveyId: String): List<QuestionEntity>
}