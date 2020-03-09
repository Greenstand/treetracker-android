package org.greenstand.android.TreeTracker.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1,2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val sql = """CREATE TABLE `planter_account` (`planter_info_id` INTEGER PRIMARY KEY NOT NULL,
            |`uploaded_tree_count` INTEGER NOT NULL, `validated_tree_count` INTEGER NOT NULL, 
            |`total_amount_paid` REAL NOT NULL, `payment_amount_pending` REAL NOT NULL,
            |`updated_at` INTEGER NOT NULL)
        """.trimMargin()
        database.execSQL(sql)
    }
}
