package org.greenstand.android.TreeTracker.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val sql = """ALTER TABLE ${TreeCaptureEntity.TABLE}
            | ADD COLUMN ${TreeCaptureEntity.WALLET} TEXT NOT NULL DEFAULT ''
            | """.trimMargin()
        database.execSQL(sql)
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val sql = """CREATE INDEX `index_planter_check_in_local_photo_path`
                    | ON `planter_check_in` (`local_photo_path`)
                    | """.trimMargin()
        database.execSQL(sql)
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.beginTransaction()
            val createLocationData = """
                CREATE TABLE `location_data` (
                    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `json_value` TEXT NOT NULL,
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val sql = """ALTER TABLE ${PlanterInfoEntity.TABLE}
            | ADD COLUMN ${PlanterInfoEntity.RECORD_UUID} TEXT NOT NULL DEFAULT ''
            | """.trimMargin()
        database.execSQL(sql)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {

        try {
            database.beginTransaction()

            // Create version 4 entities (tables)
            val createPlanterInfo = """CREATE TABLE IF NOT EXISTS `planter_info` (
                | `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                | `planter_identifier` TEXT NOT NULL,
                | `first_name` TEXT NOT NULL,
                | `last_name` TEXT NOT NULL,
                | `organization` TEXT,
                |  `phone` TEXT,
                |  `email` TEXT,
                |  `latitude` REAL NOT NULL,
                |  `longitude` REAL NOT NULL,
                |  `uploaded` INTEGER NOT NULL,
                |  `created_at` INTEGER NOT NULL,
                |  `bundle_id` TEXT
                |  )""".trimMargin()
            database.execSQL(createPlanterInfo)
            val createIndexPlanterIdentifier = """
                | CREATE INDEX IF NOT EXISTS 
                | `index_planter_identifier` ON `planter_info` (`planter_identifier`)
                | """.trimMargin()
            database.execSQL(createIndexPlanterIdentifier)
            val createIndexUploaded = """
                | CREATE INDEX IF NOT EXISTS `index_planter_uploaded`
                | ON `planter_info` (`uploaded`)
                | """.trimMargin()
            database.execSQL(createIndexUploaded)

            val createPlanterCheckIn = """CREATE TABLE IF NOT EXISTS `planter_check_in` (
                | `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                | `planter_info_id` INTEGER NOT NULL,
                | `local_photo_path` TEXT,
                | `photo_url` TEXT,
                | `latitude` REAL NOT NULL,
                | `longitude` REAL NOT NULL,
                | `created_at` INTEGER NOT NULL,
                | FOREIGN KEY(`planter_info_id`) REFERENCES `planter_info`(`_id`)
                | ON UPDATE CASCADE ON DELETE NO ACTION
                | )""".trimMargin()
            database.execSQL(createPlanterCheckIn)

            val createIndexPlanterInfo = """CREATE INDEX IF NOT EXISTS
                |`index_planter_check_in_planter_info_id`
                | ON `planter_check_in` (`planter_info_id`)""".trimMargin()
            database.execSQL(createIndexPlanterInfo)

            val createTreeCapture = """CREATE TABLE IF NOT EXISTS `tree_capture` (
                | `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                | `uuid` TEXT NOT NULL,
                | `planter_checkin_id` INTEGER NOT NULL,
                | `local_photo_path` TEXT,
                | `photo_url` TEXT,
                | `note_content` TEXT NOT NULL,
                | `latitude` REAL NOT NULL,
                | `longitude` REAL NOT NULL,
                | `accuracy` REAL NOT NULL,
                | `uploaded` INTEGER NOT NULL,
                | `created_at` INTEGER NOT NULL,
                | `bundle_id` TEXT,
                | FOREIGN KEY(`planter_checkin_id`) 
                | REFERENCES `planter_check_in`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )
                | """.trimMargin()
            database.execSQL(createTreeCapture)

            val createPlanterCheckInIndex = """CREATE INDEX IF NOT EXISTS
                | `index_tree_capture_planter_checkin_id` ON `tree_capture` (`planter_checkin_id`)
                | """.trimMargin()
            database.execSQL(createPlanterCheckInIndex)

            val createPlanterCheckInUploadIndex = """CREATE INDEX IF NOT EXISTS
                | `index_tree_capture_uploaded` ON `tree_capture` (`uploaded`)
                | """.trimMargin()
            database.execSQL(createPlanterCheckInUploadIndex)

            val createTreeAttribute = """CREATE TABLE IF NOT EXISTS `tree_attribute` (
                | `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                | `key` TEXT NOT NULL, `value` TEXT NOT NULL,
                | `tree_capture_id` INTEGER NOT NULL,
                | FOREIGN KEY(`tree_capture_id`) REFERENCES `tree_capture`(`_id`)
                | ON UPDATE CASCADE ON DELETE NO ACTION )
                | """.trimMargin()
            database.execSQL(createTreeAttribute)

            val createIndexTreeCapture = """CREATE INDEX IF NOT EXISTS 
                |`index_tree_attribute_tree_capture_id` 
                | ON `tree_attribute` (`tree_capture_id`)
                | """.trimMargin()
            database.execSQL(createIndexTreeCapture)

            // Populate Planter registrations
            val populatePlanterInfoEntity = """INSERT INTO `planter_info` (
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
            | substr(`location`, instr(`location`, ',')+1), `time_created`
            | FROM `planter_identifications`
            | """.trimMargin()
            database.execSQL(populatePlanterCheckIns)

            // Populate tree captures
            val populateTreeCaptures = """INSERT INTO `tree_capture` (`_id`, `uuid`,
            | `planter_checkin_id`, `local_photo_path`, `note_content`,
            | `latitude`, `longitude`, `accuracy`, `uploaded`, `created_at`) SELECT
            | `tree`.`_id`, `tree`.`uuid`, `tree`.`planter_identification_id`, `photo`.`name`,
            | `note`.`content`, `location`.`lat`, `location`.`long`,
            | `location`.`accuracy`, `tree`.`is_synced`, strftime('%s', `tree`.`time_created`)
            | FROM `tree`
            | LEFT OUTER JOIN `location` ON `location`.`_id` = `tree`.`location_id`
            | LEFT OUTER JOIN `tree_photo` ON `tree`.`_id` = `tree_photo`.`tree_id`
            | LEFT OUTER JOIN `photo` ON `photo`.`_id` = `tree_photo`.`photo_id`
            | LEFT OUTER JOIN `tree_note` ON `tree`.`_id` = `tree_note`.`tree_id`
            | LEFT OUTER JOIN `note` ON `note`.`_id` = `tree_note`.`note_id`
            | LEFT OUTER JOIN `planter_identifications`
            | ON `planter_identifications`.`_id` = `tree`.`planter_identification_id`
            | """.trimMargin()
            database.execSQL(populateTreeCaptures)

            val populateTreeAttributes = """INSERT INTO `tree_attribute` (
            | `_id`, `key`, `value`, `tree_capture_id`) SELECT `_id`, `key`, `value`, `tree_id`
            | FROM `tree_attributes`
            | """.trimMargin()
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
