package org.greenstand.android.TreeTracker.database.dao

import androidx.room.*
import org.greenstand.android.TreeTracker.database.entity.TreeEntity

@Dao
interface TreeDao {

    @Transaction
    @Query(
        """SELECT
            tree.uuid as uuid,
            tree._id as tree_id,
            tree.time_created as tree_time_created,
            tree.is_synced as isTreeSynced,
            location.lat as latitude,
            location.long as longitude,
            location.accuracy,
            photo.name,
            note.content as note,
            planter_identifications.identifier as planter_identifier,
            planter_identifications.photo_path as planter_photo_path,
            planter_identifications.photo_url as planter_photo_url,
            planter_identifications._id as planter_identifications_id
            FROM tree
            LEFT OUTER JOIN location ON location._id = tree.location_id
            LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id
            LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id
            LEFT OUTER JOIN tree_note ON tree._id = tree_note.tree_id
            LEFT OUTER JOIN note ON note._id = tree_note.note_id
            LEFT OUTER JOIN planter_identifications ON  planter_identifications._id = tree.planter_identification_id
            WHERE is_synced = 0"""
    )
    fun getTreesToUpload(): List<TreeDto>

    @Transaction
    @Query(
        "select tree._id as tree_id, tree.time_created as tree_time_created,tree.is_synced as isTreeSynced, location.lat as latitude, location.long as longitude, location.accuracy from tree left outer join location on location_id = location._id where is_missing = 0"
    )
    fun getTreesToDisplay(): List<TreeDto>

    @Transaction
    @Query("SELECT * FROM tree WHERE is_missing = 1 AND _id = :tree_id")
    fun getMissingTreeByID(tree_id: Long): List<TreeEntity>

    @Delete
    fun deleteTree(missingTree: TreeEntity)

    @Transaction
    @Query("SELECT * FROM tree WHERE _id = :tree_id")
    fun getTreeByID(tree_id: Long): TreeEntity

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateTree(treeEntity: TreeEntity)

    @Transaction
    @Query("SELECT COUNT(*)  FROM tree")
    fun getTotalTreeCount(): Int

    @Transaction
    @Query("SELECT COUNT(*)  FROM tree WHERE is_synced = 1")
    fun getSyncedTreeCount(): Int

    @Transaction
    @Query("SELECT COUNT(*)  FROM tree WHERE is_synced = 0")
    fun getToSyncTreeCount(): Int

    @Transaction
    @Query("SELECT DISTINCT tree_id FROM pending_updates WHERE tree_id NOT NULL and tree_id <> 0")
    fun getPendingUpdates(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treeEntity: TreeEntity): Long

    @Transaction
    @Query(
        "SELECT tree._id as tree_id, tree.time_created as tree_time_created, tree.is_synced as isTreeSynced, location.lat as latitude, location.long as longitude, location.accuracy, photo.name from tree left outer join location on location._id = tree.location_id left outer join tree_photo on tree._id = tree_id left outer join photo on photo._id = photo_id where is_outdated = 0 and tree._id = :treeIdStr"
    )
    fun getUpdatedTrees(treeIdStr: String?): List<TreeDto>

    @Transaction
    @Query("SELECT tree._id as tree_id, tree.time_created as tree_time_created,tree.time_updated as tree_time_updated,tree.time_for_update, tree.is_synced as isTreeSynced, location.lat as latitude, location.long as longitude, location.accuracy, photo.name,photo.is_outdated as isOutdated from tree left outer join location on location._id = tree.location_id left outer join tree_photo on tree._id = tree_id left outer join photo on photo._id = photo_id where tree._id =:tree_id")
    fun getTreeDtoByID(tree_id: Long): TreeDto

}

