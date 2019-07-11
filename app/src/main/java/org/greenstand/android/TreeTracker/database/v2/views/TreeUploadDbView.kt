package org.greenstand.android.TreeTracker.database.v2.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureEntity

@DatabaseView("""
        SELECT tree_capture._id as "treeCaptureId",
        tree_capture.photo_url as "treePhotoUrl",
        tree_capture.created_at,
        tree_capture.note_content,
        tree_capture.uploaded,
        tree_capture.latitude,
        tree_capture.longitude,
        tree_capture.accuracy,
        planter_check_in.identifier as "identifier",
        planter_info._id as "planterInfoId",
        planter_check_in.photo_url as "planterPhotoUrl"
        from tree_capture
        left outer join planter_check_in on planter_check_in._id = tree_capture.planter_checkin_id
        left outer join planter_info on planter_info._id = planter_check_in.planter_info_id""")
data class TreeUploadDbView(
    @ColumnInfo(name = TreeCaptureEntity.UUID)
    var uuid: String,
    @ColumnInfo(name = "identifier")
    var identifier: String,
    @ColumnInfo(name = "treePhotoUrl")
    var treePhotoUrl: String,
    @ColumnInfo(name = "planterInfoId") // TODO CHECK WHAT THIS VALUE SHOULD BE
    var planterInfoId: String,
    @ColumnInfo(name = "treeCaptureId")
    var treeCaptureId: Long,
    @ColumnInfo(name = "planterPhotoUrl")
    var planterPhotoUrl: String,
    @ColumnInfo(name = TreeCaptureEntity.LATITUDE)
    var latitude: Double,
    @ColumnInfo(name = TreeCaptureEntity.LONGITUDE)
    var longitude: Double,
    @ColumnInfo(name = TreeCaptureEntity.NOTE_CONTENT)
    var noteContent: String,
    @ColumnInfo(name = TreeCaptureEntity.ACCURACY)
    var accuracy: Double,
    @ColumnInfo(name = TreeCaptureEntity.CREATED_AT)
    var createAt: Long
)