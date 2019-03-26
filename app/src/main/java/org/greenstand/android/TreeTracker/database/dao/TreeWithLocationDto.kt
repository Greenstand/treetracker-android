package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Embedded
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity

class TreeWithLocationDto {
    @Embedded
    lateinit var treeEntity: TreeEntity
    @Embedded
    lateinit var locationEntity: LocationEntity

}

