package org.greenstand.android.TreeTracker.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.beginTransaction()
            val createTableVersionNew = """
                |CREATE TABLE `planter_check_in_new` (
                |   `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                |   `planter_info_id` INTEGER NOT NULL,
                |   `local_photo_path` TEXT,
                |   `photo_url` TEXT,
                |   `latitude` REAL NOT NULL,
                |   `longitude` REAL NOT NULL,
                |   `created_at` INTEGER NOT NULL,
                |    FOREIGN KEY (`planter_info_id`) REFERENCES `planter_info`(`_id`) 
                |    ON UPDATE CASCADE ON DELETE NO ACTION )
                |    """.trimMargin()
            database.execSQL(createTableVersionNew)

            val populateNewTable = """INSERT INTO `planter_check_in_new` (
                |   `_id`, `planter_info_id`, `local_photo_path`, `photo_url`, 
                |   `latitude`, `longitude`, `created_at`
                | )  SELECT `_id`, `planter_info_id`, `local_photo_path`, `photo_url`, 
                |   `latitude`, `longitude`, `created_at` FROM `planter_check_in`
            """.trimMargin()
            database.execSQL(populateNewTable)

            // Drop the original table
            database.execSQL("DROP TABLE `planter_check_in`")

            // Rename the new table to be same as the original table
            database.execSQL("ALTER TABLE `planter_check_in_new` RENAME TO `planter_check_in`")

            // Recreate indexes
            val indexCreation = """
                |  CREATE INDEX `index_planter_check_in_planter_info_id` ON 
                |  `planter_check_in` (`planter_info_id`)
                |  """.trimMargin()
            database.execSQL(indexCreation)

            // Add a new column to `planter_info`
            database.execSQL("ALTER TABLE `planter_info` ADD COLUMN `bundle_id` TEXT")

            if (database.isDatabaseIntegrityOk) {
                database.setTransactionSuccessful()
            }
        } finally {
            database.endTransaction()
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.beginTransaction()
            val createLocationData = """
                CREATE TABLE `location_data` (
                    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `base64_json` TEXT NOT NULL,
                    `uploaded` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
            """.trimIndent()
            database.execSQL(createLocationData)
            val indexCreation = "CREATE INDEX `index_upload_id` ON `location_data` (`uploaded`)"
            database.execSQL(indexCreation)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}