package org.greenstand.android.TreeTracker.models.messages.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SurveyEntity::class,
            parentColumns = ["id"],
            childColumns = ["survey_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
class QuestionEntity(
    @ColumnInfo(name = "survey_id", index = true)
    val surveyId: String,
    @ColumnInfo(name = "prompt")
    val prompt: String,
    @ColumnInfo(name = "choices")
    val choices: List<String>,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}