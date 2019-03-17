package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TreeDao {

    @Transaction
    @Query(
        "SELECT " +
                "tree._id as tree_id, " +
                "tree.time_created as tree_time_created, " +
                "tree.is_synced as isTreeSynced, " +
                "location.lat, " +
                "location.long, " +
                "location.accuracy, " +
                "photo.name, " +
                "note.content, " +
                "planter_identifications.identifier as planter_identifier, " +
                "planter_identifications.photo_path as planter_photo_path, " +
                "planter_identifications.photo_url as planter_photo_url, " +
                "planter_identifications._id as planter_identifications_id " +
                "FROM tree " +
                "LEFT OUTER JOIN location ON location._id = tree.location_id " +
                "LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id " +
                "LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id " +
                "LEFT OUTER JOIN tree_note ON tree._id = tree_note.tree_id " +
                "LEFT OUTER JOIN note ON note._id = tree_note.note_id " +
                "LEFT OUTER JOIN planter_identifications ON  planter_identifications._id = tree.planter_identification_id  " +
                "WHERE " +
                "is_synced = 0"
    )
    suspend fun getTreesToUpload(): List<TreeDto>


}

