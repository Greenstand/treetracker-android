package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree")
data class TreeEntity(@PrimaryKey
                      @ColumnInfo(name = "id")
                      var id: Long,
                      @ColumnInfo(name = "location_id")
                      var locationId: Long,
                      @ColumnInfo(name = "planter_identification_id")
                      var planterId: Long,
                      @ColumnInfo(name = "time_created")
                      var timeCreated: Long,
                      @ColumnInfo(name = "time_updated")
                      var timeUpdated: Long,
                      @ColumnInfo(name = "time_for_update")
                      var timeForUpdate: Long,
                      @ColumnInfo(name = "is_synced")
                      var isSynced: Boolean,
                      @ColumnInfo(name = "is_priority")
                      var isPriority: Boolean,
                      @ColumnInfo(name = "cause_of_death_id")
                      var causeOfDeath: Int,
                      @ColumnInfo(name = "main_db_id")
                      var mainDbId: Long,
                      @ColumnInfo(name = "settings_id"))