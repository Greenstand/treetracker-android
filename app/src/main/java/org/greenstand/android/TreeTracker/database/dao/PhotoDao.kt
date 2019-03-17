package org.greenstand.android.TreeTracker.database.dao

import androidx.room.*
import org.greenstand.android.TreeTracker.database.entity.PhotoEntity
import org.greenstand.android.TreeTracker.database.entity.TreePhotoEntity

@Dao
interface PhotoDao {
    @Transaction
    @Query("SELECT * FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = :tree_id")
    fun getPhotosByTreeId(tree_id: Long): List<PhotoEntity>

    @Transaction
    @Query("SELECT * FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 1 and tree_id = :tree_id")
    fun getOutdatedPhotos(tree_id: Long): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treePhotoEntity: TreePhotoEntity)

}