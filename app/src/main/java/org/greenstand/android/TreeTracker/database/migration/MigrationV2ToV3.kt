package org.greenstand.android.TreeTracker.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


class MigrationV2ToV3 : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE planter_identifications ADD COLUMN location TEXT")
        database.execSQL("ALTER TABLE planter_details ADD COLUMN location TEXT")
    }

}