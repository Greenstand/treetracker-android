package org.greenstand.android.TreeTracker.database.entities

import android.provider.ContactsContract
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = NoteEntity.TABLE)
data class NoteEntity(@PrimaryKey
                      @ColumnInfo(name = ID)
                      var id: Long,
                      @ColumnInfo(name = MAIN_DB_ID)
                      var mainDbId: Long?,
                      @ColumnInfo(name = CONTENT)
                      var content: String?,
                      @ColumnInfo(name = TIME_CREATED)
                      var timeCreated: Long?,
                      @ColumnInfo(name = USER_ID)
                      var userId: Long?) {


    companion object {
        const val TABLE = "note"
        const val ID = "note"
        const val MAIN_DB_ID = "main_db_id"
        const val CONTENT = "content"
        const val TIME_CREATED = "time_created"
        const val USER_ID = "user_id"
    }
}

// TableInfo{name='tree', columns={is_priority=Column{name='is_priority', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, planter_identification_id=Column{name='planter_identification_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, cause_of_death_id=Column{name='cause_of_death_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, is_synced=Column{name='is_synced', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, location_id=Column{name='location_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, time_updated=Column{name='time_updated', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, main_db_id=Column{name='main_db_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, settings_id=Column{name='settings_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, user_id=Column{name='user_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, time_for_update=Column{name='time_for_update', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, time_created=Column{name='time_created', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, is_missing=Column{name='is_missing', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, _id=Column{name='_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=1}, settings_override_id=Column{name='settings_override_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, three_digit_number=Column{name='three_digit_number', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}}, foreignKeys=[ForeignKey{referenceTable='location', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[location_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='location', onDelete='NO ACTION', onUpdate='NO ACTION', columnNames=[location_id], referenceColumnNames=[_id]}], indices=[]}
//

// TableInfo{name='tree', columns={is_priority=Column{name='is_priority', type='BOOLEAN', affinity='1', notNull=false, primaryKeyPosition=0}, planter_identification_id=Column{name='planter_identification_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, cause_of_death_id=Column{name='cause_of_death_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, is_synced=Column{name='is_synced', type='BOOLEAN', affinity='1', notNull=false, primaryKeyPosition=0}, time_updated=Column{name='time_updated', type='TIMESTAMP', affinity='1', notNull=false, primaryKeyPosition=0}, location_id=Column{name='location_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, main_db_id=Column{name='main_db_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, settings_id=Column{name='settings_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, user_id=Column{name='user_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, time_for_update=Column{name='time_for_update', type='TIMESTAMP', affinity='1', notNull=false, primaryKeyPosition=0}, time_created=Column{name='time_created', type='TIMESTAMP', affinity='1', notNull=false, primaryKeyPosition=0}, is_missing=Column{name='is_missing', type='BOOLEAN', affinity='1', notNull=false, primaryKeyPosition=0}, _id=Column{name='_id', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=1}, settings_override_id=Column{name='settings_override_id', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, three_digit_number=Column{name='three_digit_number', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}}, foreignKeys=[ForeignKey{referenceTable='users', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[user_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='location', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[location_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='settings', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[settings_override_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='note', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[cause_of_death_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='planter_identifications', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[planter_identification_id], referenceColumnNames=[_id]}, ForeignKey{referenceTable='settings', onDelete='NO ACTION', onUpdate='CASCADE', columnNames=[settings_id], referenceColumnNames=[_id]}], indices=[]}
//