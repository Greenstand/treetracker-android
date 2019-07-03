package org.greenstand.android.TreeTracker.database.v2.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

inline class TreeCaptureId(val value: Long)

@Entity(
    tableName = TreeCaptureEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PlanterCheckInEntity::class,
            parentColumns = [PlanterCheckInEntity.ID],
            childColumns = [TreeCaptureEntity.PLANTER_CHECK_IN_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
class TreeCaptureEntity(
    @ColumnInfo(name = UUID)
    var uuid: String,
    @ColumnInfo(name = PLANTER_CHECK_IN_ID)
    var planterCheckInId: PlanterCheckInId,
    @ColumnInfo(name = LOCAL_PHOTO_PATH)
    var localPhotoPath: String,
    @ColumnInfo(name = PHOTO_URL)
    var photoUrl: String?,
    @ColumnInfo(name = NOTE_CONTENT)
    var noteContent: String,
    @ColumnInfo(name = LATITUDE)
    var latitude: Long,
    @ColumnInfo(name = LONGITUDE)
    var longitude: Long,
    @ColumnInfo(name = ACCURACY)
    var accuracy: Long,
    @ColumnInfo(name = UPLOADED)
    var uploaded: Boolean = false,
    @ColumnInfo(name = CREATED_AT)
    var createAt: String
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: TreeCaptureId = TreeCaptureId(0)

    companion object {
        const val TABLE = "tree_capture"

        const val ID = "_id"
        const val UUID = "uuid"
        const val PLANTER_CHECK_IN_ID = "planter_checkin_id"
        const val LOCAL_PHOTO_PATH = "local_photo_path"
        const val PHOTO_URL = "photo_url"
        const val NOTE_CONTENT = "note_content"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ACCURACY = "accuracy"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
    }
}