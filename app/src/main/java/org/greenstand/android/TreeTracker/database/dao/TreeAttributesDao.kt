package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributesEntity

@Dao
interface TreeAttributesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treeAttributesEntity: TreeAttributesEntity): Long

    @Query("SELECT * FROM tree_attributes WHERE tree_id = :treeId AND `key` = :key")
    fun getTreeAttributeByTreeAndKey(treeId: Long, key: String): TreeAttributesEntity?

}