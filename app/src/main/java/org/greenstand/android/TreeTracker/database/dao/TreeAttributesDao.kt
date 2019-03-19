package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributesEntity

@Dao
interface TreeAttributesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treeAttributesEntity: TreeAttributesEntity): Long

}