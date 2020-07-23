package org.greenstand.android.TreeTracker.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {

        try {
            database.beginTransaction()
            // Populate Planter registrations
            val populatePlanterInfoEntity  = """INSERT INTO `planter_info` (
            | `_id`, `planter_identifier`, `first_name`, `last_name`, `organization`,
            | `phone`, `email`, `latitude`, `longitude`, `uploaded`, `created_at`)
            | SELECT `_id`, `identifier`, `first_name`, `last_name`, `organization`,
            | `phone`, `email`, substr(`location`, 0, instr(`location`, ',')), 
            |  substr(`location`, instr(`location`, ',')+1), `uploaded`, 
            |  strftime('%s', `time_created`) FROM `planter_details`
            | """.trimMargin()
            database.execSQL(populatePlanterInfoEntity)

            // Populate planter checkins
            val populatePlanterCheckIns = """INSERT INTO `planter_check_in` (
            | `_id`, `planter_info_id`, `local_photo_path`, `photo_url`,
            | `latitude`, `longitude`, `created_at`) SELECT `_id`, `planter_details_id`,
            | `photo_path`, `photo_url`, substr(`location`, 0, instr(`location`, ',')),
            | substr(`location`, instr(`location`, ',')+1), strftime('%s', `time_created`)
            | FROM `planter_identifications`
        """.trimMargin()
            database.execSQL(populatePlanterCheckIns)

            // Populate tree captures
            val populateTreeCaptures = """INSERT INTO `tree_capture` (`_id`, `uuid`,
            | `planter_checkin_id`, `local_photo_path`, `note_content`,
            | `latitude`, `longitude`, `accuracy`, `uploaded`, `created_at`) SELECT
            | `tree`.`_id`, `tree`.`uuid`, `tree`.`planter_identification_id`, `photo`.`name`,
            | `note`.`content`, `location`.`latitude`, `location`.`longitude`,
            | `location`.`accuracy`, `tree`.`is_synced`, strftime('%s', `tree`.`time_created`)
            | FROM `tree`
            | LEFT OUTER JOIN `location` ON `location`.`_id` = `tree`.`location_id`
            | LEFT OUTER JOIN `tree_photo` ON `tree`.`_id` = `tree_photo`.`tree_id`
            | LEFT OUTER JOIN `photo` ON `photo`.`_id` = `tree_photo`.`photo_id`
            | LEFT OUTER JOIN `tree_note` ON `tree`.`_id` = `tree_note`.`tree_id`
            | LEFT OUTER JOIN `note` ON `note`.`_id` = `tree_note`.`note_id`
            | LEFT OUTER JOIN `planter_identifications`
            | ON `planter_identifications`.`_id` = `tree`.`planter_identification_id`
        """.trimMargin()
            database.execSQL(populateTreeCaptures)

            val populateTreeAttributes = """INSERT INTO `tree_attribute` (
            | `_id`, `key`, `value`, `tree_capture_id`) SELECT `_id`, `key`, `value`, `tree_id`
            | FROM `tree_attributes`
        """.trimMargin()
            database.execSQL(populateTreeAttributes)


            // Drop tables found in 1.2.7 release branch
            val deletePendingUpdates = "drop table if exists `pending_updates`"
            database.execSQL(deletePendingUpdates)

            val dropTreeAttributes = "drop table if exists `tree_attributes`"
            database.execSQL(dropTreeAttributes)

            val dropTreePhoto = "drop table if exists `tree_photo`"
            database.execSQL(dropTreePhoto)

            val dropNoteEntity = "drop table if exists `tree_note`"
            database.execSQL(dropNoteEntity)

            val dropTree = "drop table if exists `tree`"
            database.execSQL(dropTree)

            val dropSettings = "drop table if exists `settings`"
            database.execSQL(dropSettings)

            val dropPlanterIdentifications = "drop table if exists `planter_identifications`"
            database.execSQL(dropPlanterIdentifications)

            val dropPlanterDetails = "drop table if exists `planter_details`"
            database.execSQL(dropPlanterDetails)

            val dropPhoto = "drop table if exists `photo`"
            database.execSQL(dropPhoto)

            val dropNote = "drop table if exists `note`"
            database.execSQL(dropNote)

            val dropLocation = "drop table if exists `location`"
            database.execSQL(dropLocation)

            if (database.isDatabaseIntegrityOk) {
                database.setTransactionSuccessful()
            }
        } finally {
            database.endTransaction()
        }
    }
}

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