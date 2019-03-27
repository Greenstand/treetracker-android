package org.greenstand.android.TreeTracker.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV1ToV2 : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        //new table
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS
            `tree_attributes`
            (`_id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `height_color` TEXT,
            `flavor_id` TEXT NOT NULL,
            `app_version` TEXT NOT NULL,
            `app_build` TEXT NOT NULL)""")

        //deprecated tables
        database.execSQL("drop table global_settings")

        //migrate data to tree table
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `new_tree`
                (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `main_db_id` INTEGER NOT NULL,
                `time_created` TEXT NOT NULL,
                `time_updated` TEXT NOT NULL,
                `time_for_update` TEXT NOT NULL,
                `three_digit_number` INTEGER,
                `location_id` INTEGER,
                `is_missing` INTEGER NOT NULL,
                `is_synced` INTEGER NOT NULL,
                `is_priority` INTEGER NOT NULL,
                `cause_of_death_id` INTEGER,
                `settings_id` INTEGER,
                `settings_override_id` INTEGER,
                `user_id` INTEGER,
                `planter_identification_id` INTEGER,
                `attributes_id` INTEGER,
                FOREIGN KEY(`settings_override_id`) REFERENCES `settings`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
                FOREIGN KEY(`settings_id`) REFERENCES `settings`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
                FOREIGN KEY(`location_id`) REFERENCES `location`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
                FOREIGN KEY(`cause_of_death_id`) REFERENCES `note`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
                FOREIGN KEY(`planter_identification_id`) REFERENCES `planter_identifications`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
                FOREIGN KEY(`attributes_id`) REFERENCES `tree_attributes`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION)"""
        )

        database.execSQL("""CREATE TABLE IF NOT EXISTS `tree`
            (`_id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `main_db_id` INTEGER,
            `time_created` INTEGER,
            `time_updated` INTEGER,
            `time_for_update` INTEGER,
            `three_digit_number` INTEGER,
            `location_id` INTEGER,
            `is_missing` INTEGER NOT NULL,
            `is_synced` INTEGER NOT NULL,
            `is_priority` INTEGER NOT NULL,
            `cause_of_death_id` INTEGER,
            `settings_id` INTEGER,
            `settings_override_id` INTEGER,
            `user_id` INTEGER,
            `planter_identification_id` INTEGER,
            FOREIGN KEY(`settings_override_id`) REFERENCES `settings`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
            FOREIGN KEY(`settings_id`) REFERENCES `settings`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
            FOREIGN KEY(`location_id`) REFERENCES `location`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
            FOREIGN KEY(`cause_of_death_id`) REFERENCES `note`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION ,
            FOREIGN KEY(`planter_identification_id`) REFERENCES `planter_identifications`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION)""")

        database.execSQL(
            "INSERT INTO new_tree(_id,main_db_id, time_created, time_updated, time_for_update, three_digit_number, location_id, is_missing, is_synced, is_priority, cause_of_death_id, settings_id, settings_override_id, user_id, planter_identification_id)  SELECT _id, main_db_id, time_created, time_updated, time_for_update, three_digit_number, location_id, CASE WHEN (is_missing = 'N') THEN 0 ELSE 1 END, CASE WHEN (is_synced like 'N') THEN 0 ELSE 1 END, CASE WHEN (is_priority like 'N') THEN 0 ELSE 1 END, cause_of_death_id, settings_id, settings_override_id, user_id, planter_identification_id FROM tree"
        )
        database.execSQL("drop table tree")
        database.execSQL("alter table new_tree RENAME TO  tree")

        //migrate planter details table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_planter_details` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `identifier` TEXT, `first_name` TEXT, `last_name` TEXT, `organization` TEXT, `phone` TEXT, `email` TEXT, `uploaded` INTEGER NOT NULL, `time_created` TEXT NOT NULL)")
        database.execSQL(
            "INSERT INTO new_planter_details(_id,identifier, first_name, last_name, organization, phone, email, uploaded, time_created) SELECT _id, identifier, first_name, last_name, organization, phone, email, CASE WHEN (uploaded = 'N') THEN 0 ELSE 1 END, time_created FROM planter_details"
        )
        database.execSQL("drop table planter_details")
        database.execSQL("alter table new_planter_details RENAME TO  planter_details")

        //migrate photo table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_photo` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `location_id` INTEGER, `main_db_id` INTEGER NOT NULL, `is_outdated` INTEGER NOT NULL, `time_taken` TEXT NOT NULL, `user_id` INTEGER, FOREIGN KEY(`location_id`) REFERENCES `location`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )")
        database.execSQL(
            "INSERT INTO new_photo (_id,name, location_id, main_db_id, is_outdated, time_taken, user_id) SELECT _id,name, main_db_id,location_id, CASE WHEN (is_outdated = 'N') THEN 0 ELSE 1 END, time_taken,  user_id FROM photo"
        )
        database.execSQL("drop table photo")
        database.execSQL("alter table new_photo RENAME TO  photo")

        //migrate tree note table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_tree_note` (`note_id` INTEGER NOT NULL, `tree_id` INTEGER NOT NULL, PRIMARY KEY(`note_id`, `tree_id`), FOREIGN KEY(`note_id`) REFERENCES `note`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`tree_id`) REFERENCES `tree`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )")
        database.execSQL(
            "INSERT INTO new_tree_note(note_id, tree_id) SELECT note_id,tree_id FROM tree_note"
        )
        database.execSQL("drop table tree_note")
        database.execSQL("alter table new_tree_note RENAME TO  tree_note")

        //migrate tree photo table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_tree_photo` (`tree_id` INTEGER NOT NULL, `photo_id` INTEGER NOT NULL, PRIMARY KEY(`tree_id`, `photo_id`), FOREIGN KEY(`photo_id`) REFERENCES `photo`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`tree_id`) REFERENCES `tree`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )");
        database.execSQL(
            "INSERT INTO new_tree_photo(photo_id, tree_id) SELECT photo_id,tree_id FROM tree_photo"
        )
        database.execSQL("drop table tree_photo")
        database.execSQL("alter table new_tree_photo RENAME TO  tree_photo")

        //migrate location table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_location` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accuracy` INTEGER, `lat` REAL, `long` REAL, `user_id` INTEGER, `main_db_id` INTEGER NOT NULL)")
        database.execSQL(
            "INSERT INTO new_location(_id,accuracy, lat, long, user_id, main_db_id) SELECT _id, cast(accuracy AS INTEGER), lat, long,  user_id,main_db_id FROM location"
        )
        database.execSQL("drop table location")
        database.execSQL("alter table new_location RENAME TO  location")

        //migrate table note
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_note` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `main_db_id` INTEGER NOT NULL, `content` TEXT, `time_created` TEXT NOT NULL, `user_id` INTEGER)")
        database.execSQL(
            "INSERT INTO new_note(_id, main_db_id, content, time_created, user_id) SELECT _id, main_db_id, content, time_created, user_id FROM note"
        )
        database.execSQL("drop table note")
        database.execSQL("alter table new_note RENAME TO  note")

        //migrate planter ids table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_planter_identifications` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planter_details_id` INTEGER, `identifier` TEXT, `photo_path` TEXT, `photo_url` TEXT, `time_created` TEXT NOT NULL, FOREIGN KEY(`planter_details_id`) REFERENCES `planter_details`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )")
        database.execSQL(
            "INSERT INTO new_planter_identifications(_id,planter_details_id, identifier, photo_path, photo_url, time_created) SELECT _id, planter_details_id, identifier, photo_path, photo_url, time_created FROM planter_identifications"
        )
        database.execSQL("drop table planter_identifications")
        database.execSQL("alter table new_planter_identifications RENAME TO  planter_identifications")

        //migrate settings table
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_settings` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `main_db_id` INTEGER NOT NULL, `time_to_next_update` INTEGER NOT NULL, `min_accuracy` INTEGER NOT NULL)")
        database.execSQL(
            "INSERT INTO new_settings(_id,main_db_id, time_to_next_update, min_accuracy) SELECT _id, main_db_id, time_to_next_update, min_accuracy FROM settings"
        )
        database.execSQL("drop table settings")
        database.execSQL("alter table new_settings RENAME TO  settings")

        //migrate pending updates
        database.execSQL("CREATE TABLE IF NOT EXISTS `new_pending_updates` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `main_db_id` INTEGER NOT NULL, `user_id` INTEGER, `settings_id` INTEGER, `tree_id` INTEGER, `location_id` INTEGER, `radius` INTEGER, FOREIGN KEY(`settings_id`) REFERENCES `settings`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`tree_id`) REFERENCES `tree`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`location_id`) REFERENCES `location`(`_id`) ON UPDATE CASCADE ON DELETE NO ACTION )");
        database.execSQL(
            "INSERT INTO new_pending_updates(_id,main_db_id, user_id, settings_id, tree_id, location_id, radius) SELECT _id, main_db_id, user_id, settings_id, tree_id, location_id, radius FROM pending_updates"
        )
        database.execSQL("drop table pending_updates")
        database.execSQL("alter table new_pending_updates RENAME TO  pending_updates")

    }
}