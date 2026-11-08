/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the hand-written migrations (MIGRATION_3_4 to MIGRATION_6_7) directly against a raw database,
 * and the whole chain from a 1.2.x-era (schema 3) database through AppDatabase.builder.
 * AppDatabaseMigrationTest covers upgrades from the exported schemas 6 and later.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ManualMigrationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun openRawDatabase() {
        helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(null)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) = Unit

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(TEST_DB)
    }

    // region MIGRATION_3_4

    @Test
    fun `MIGRATION_3_4 splits planter locations into latitude and longitude and converts dates to epoch seconds`() {
        createLegacySchema3(db::execSQL)

        MIGRATION_3_4.migrate(db)

        db.query("SELECT * FROM planter_info WHERE _id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("planter@example.com", cursor.string("planter_identifier"))
            assertEquals("Ada", cursor.string("first_name"))
            assertEquals("Lovelace", cursor.string("last_name"))
            assertEquals("Greenstand", cursor.string("organization"))
            assertEquals("+15550100", cursor.string("phone"))
            assertEquals(-1.2921, cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")), 0.0)
            assertEquals(36.8219, cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")), 0.0)
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("uploaded")))
            assertEquals(PLANTER_CREATED_EPOCH, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("bundle_id")))
        }
    }

    @Test
    fun `MIGRATION_3_4 copies planter identifications into check ins`() {
        createLegacySchema3(db::execSQL)

        MIGRATION_3_4.migrate(db)

        db.query("SELECT * FROM planter_check_in WHERE _id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("planter_info_id")))
            assertEquals("/selfie.jpg", cursor.string("local_photo_path"))
            assertEquals("https://photos/selfie.jpg", cursor.string("photo_url"))
            assertEquals(10.5, cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")), 0.0)
            assertEquals(-20.25, cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")), 0.0)
            assertEquals(CHECK_IN_CREATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
        }
    }

    @Test
    fun `MIGRATION_3_4 joins legacy trees with their location, photo and note into tree captures`() {
        createLegacySchema3(db::execSQL)

        MIGRATION_3_4.migrate(db)

        db.query("SELECT * FROM tree_capture ORDER BY _id").use { cursor ->
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals("tree-uuid-1", cursor.string("uuid"))
            assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("planter_checkin_id")))
            assertEquals("/tree-1.jpg", cursor.string("local_photo_path"))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("photo_url")))
            assertEquals("first note", cursor.string("note_content"))
            assertEquals(1.5, cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")), 0.0)
            assertEquals(2.5, cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")), 0.0)
            assertEquals(4.0, cursor.getDouble(cursor.getColumnIndexOrThrow("accuracy")), 0.0)
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("uploaded")))
            assertEquals(TREE_CREATED_EPOCH, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))

            cursor.moveToNext()
            assertEquals("tree-uuid-2", cursor.string("uuid"))
            assertTrue("A tree without a photo keeps a null path", cursor.isNull(cursor.getColumnIndexOrThrow("local_photo_path")))
            assertEquals("second note", cursor.string("note_content"))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("uploaded")))
        }
    }

    @Test
    fun `MIGRATION_3_4 copies tree attributes onto the migrated captures`() {
        createLegacySchema3(db::execSQL)

        MIGRATION_3_4.migrate(db)

        db.query("SELECT `key`, `value`, tree_capture_id FROM tree_attribute ORDER BY _id").use { cursor ->
            val rows = mutableListOf<Triple<String, String, Long>>()
            while (cursor.moveToNext()) rows += Triple(cursor.getString(0), cursor.getString(1), cursor.getLong(2))
            assertEquals(listOf(Triple("height", "3", 1L), Triple("species", "acacia", 2L)), rows)
        }
    }

    @Test
    fun `MIGRATION_3_4 drops every 1_2_x table`() {
        createLegacySchema3(db::execSQL)

        MIGRATION_3_4.migrate(db)

        val tables = tableNames(db)
        LEGACY_TABLES.forEach { assertFalse("$it should be dropped", it in tables) }
        listOf("planter_info", "planter_check_in", "tree_capture", "tree_attribute").forEach {
            assertTrue("$it should exist", it in tables)
        }
    }

    // endregion

    // region MIGRATION_4_5 .. MIGRATION_6_7

    @Test
    fun `MIGRATION_4_5 adds record_uuid with an empty default to existing and new planters`() {
        createLegacySchema3(db::execSQL)
        MIGRATION_3_4.migrate(db)

        MIGRATION_4_5.migrate(db)

        db.execSQL(
            "INSERT INTO planter_info (planter_identifier, first_name, last_name, latitude, longitude, uploaded, created_at) " +
                "VALUES ('new@example.com', 'Grace', 'Hopper', 0, 0, 0, 0)",
        )
        db.query("SELECT planter_identifier, record_uuid FROM planter_info ORDER BY _id").use { cursor ->
            val rows = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) rows += cursor.getString(0) to cursor.getString(1)
            assertEquals(listOf("planter@example.com" to "", "new@example.com" to ""), rows)
        }
    }

    @Test
    fun `MIGRATION_5_6 creates the location_data table with an uploaded index`() {
        MIGRATION_5_6.migrate(db)

        assertEquals(listOf("_id", "json_value", "uploaded", "created_at"), columnNames(db, "location_data"))
        assertEquals(listOf("uploaded"), indexedColumns(db, "location_data"))
        db.execSQL("INSERT INTO location_data (json_value, uploaded, created_at) VALUES ('{}', 0, 1)")
        db.query("SELECT COUNT(*) FROM location_data").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun `MIGRATION_6_7 indexes the check in local photo path`() {
        createLegacySchema3(db::execSQL)
        MIGRATION_3_4.migrate(db)

        MIGRATION_6_7.migrate(db)

        assertTrue("local_photo_path" in indexedColumns(db, "planter_check_in"))
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_planter_check_in_local_photo_path'").use {
            assertEquals(1, it.count)
        }
    }

    // endregion

    // region whole chain through AppDatabase

    @Test
    fun `a schema 3 database upgrades to latest through every manual and auto migration`() =
        runTest {
            createFileDatabase(version = 3) { createLegacySchema3(it::execSQL) }

            val database = AppDatabase.builder(context, TEST_DB).allowMainThreadQueries().build()
            try {
                val planter = database.planterDao().getPlanterInfoByIdentifier("planter@example.com")!!
                assertEquals(-1.2921, planter.latitude, 0.0)
                assertEquals(36.8219, planter.longitude, 0.0)
                assertEquals(PLANTER_CREATED_EPOCH, planter.createdAt)
                assertEquals("", planter.recordUuid)
                assertTrue(planter.uploaded)

                val checkIn = database.planterDao().getPlanterCheckInById(1L)!!
                assertEquals("/selfie.jpg", checkIn.localPhotoPath)

                val treeDao = database.treeDao()
                assertEquals(listOf(2L), treeDao.getAllTreeCaptureIdsToUpload())
                val capture = treeDao.getTreeCaptureById(1L)
                assertEquals("tree-uuid-1", capture.uuid)
                assertEquals("first note", capture.noteContent)
                assertEquals(TREE_CREATED_EPOCH, capture.createAt)
                assertEquals(listOf("height" to "3"), treeDao.getTreeAttributeByTreeCaptureId(1L).map { it.key to it.value })

                // The 1.2.x `tree` and `location` tables were replaced by the empty v2 tables.
                assertTrue(treeDao.getAllTrees().isEmpty())
                assertTrue(database.locationDao().getLocationData().isEmpty())
                assertTrue(database.userDao().getAllUsersList().isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `a database older than schema 3 fails to open instead of being wiped`() {
        createFileDatabase(version = 2) { it.execSQL("CREATE TABLE tree (_id INTEGER PRIMARY KEY, uuid TEXT)") }

        val database = AppDatabase.builder(context, TEST_DB).build()
        try {
            assertThrows(IllegalStateException::class.java) { database.openHelper.writableDatabase }
        } finally {
            database.close()
        }

        SQLiteDatabase.openDatabase(context.getDatabasePath(TEST_DB).path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals("The old database is left untouched", 2, it.version)
        }
    }

    // endregion

    private fun createFileDatabase(
        version: Int,
        create: (SQLiteDatabase) -> Unit,
    ) {
        val file = context.getDatabasePath(TEST_DB).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            create(database)
            database.version = version
        }
    }

    /** The 1.2.x tables MIGRATION_3_4 reads, with the columns it uses, seeded with two trees. */
    private fun createLegacySchema3(exec: (String) -> Unit) {
        exec(
            "CREATE TABLE planter_details (_id INTEGER PRIMARY KEY, identifier TEXT, first_name TEXT, last_name TEXT, " +
                "organization TEXT, phone TEXT, email TEXT, location TEXT, uploaded INTEGER, time_created TEXT)",
        )
        exec(
            "CREATE TABLE planter_identifications (_id INTEGER PRIMARY KEY, planter_details_id INTEGER, photo_path TEXT, " +
                "photo_url TEXT, location TEXT, time_created INTEGER)",
        )
        exec("CREATE TABLE location (_id INTEGER PRIMARY KEY, lat REAL, long REAL, accuracy REAL)")
        exec(
            "CREATE TABLE tree (_id INTEGER PRIMARY KEY, uuid TEXT, planter_identification_id INTEGER, location_id INTEGER, " +
                "is_synced INTEGER, time_created TEXT)",
        )
        exec("CREATE TABLE photo (_id INTEGER PRIMARY KEY, name TEXT)")
        exec("CREATE TABLE tree_photo (_id INTEGER PRIMARY KEY, tree_id INTEGER, photo_id INTEGER)")
        exec("CREATE TABLE note (_id INTEGER PRIMARY KEY, content TEXT)")
        exec("CREATE TABLE tree_note (_id INTEGER PRIMARY KEY, tree_id INTEGER, note_id INTEGER)")
        exec("CREATE TABLE tree_attributes (_id INTEGER PRIMARY KEY, `key` TEXT, `value` TEXT, tree_id INTEGER)")
        exec("CREATE TABLE pending_updates (_id INTEGER PRIMARY KEY)")
        exec("CREATE TABLE settings (_id INTEGER PRIMARY KEY)")

        exec(
            "INSERT INTO planter_details VALUES (1, 'planter@example.com', 'Ada', 'Lovelace', 'Greenstand', '+15550100', " +
                "NULL, '-1.2921,36.8219', 1, '2019-05-01 10:00:00')",
        )
        exec(
            "INSERT INTO planter_identifications VALUES (1, 1, '/selfie.jpg', 'https://photos/selfie.jpg', '10.5,-20.25', " +
                "$CHECK_IN_CREATED_AT)",
        )
        exec("INSERT INTO location VALUES (1, 1.5, 2.5, 4.0)")
        exec("INSERT INTO location VALUES (2, 3.5, 4.5, 8.0)")
        exec("INSERT INTO tree VALUES (1, 'tree-uuid-1', 1, 1, 1, '2019-05-01 10:05:00')")
        exec("INSERT INTO tree VALUES (2, 'tree-uuid-2', 1, 2, 0, '2019-05-01 10:06:00')")
        exec("INSERT INTO photo VALUES (1, '/tree-1.jpg')")
        exec("INSERT INTO tree_photo VALUES (1, 1, 1)")
        exec("INSERT INTO note VALUES (1, 'first note')")
        exec("INSERT INTO note VALUES (2, 'second note')")
        exec("INSERT INTO tree_note VALUES (1, 1, 1)")
        exec("INSERT INTO tree_note VALUES (2, 2, 2)")
        exec("INSERT INTO tree_attributes VALUES (1, 'height', '3', 1)")
        exec("INSERT INTO tree_attributes VALUES (2, 'species', 'acacia', 2)")
    }

    private fun android.database.Cursor.string(column: String): String? = getString(getColumnIndexOrThrow(column))

    private fun tableNames(database: SupportSQLiteDatabase): Set<String> =
        database.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private fun columnNames(
        database: SupportSQLiteDatabase,
        table: String,
    ): List<String> =
        database.query("PRAGMA table_info(`$table`)").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }

    private fun indexedColumns(
        database: SupportSQLiteDatabase,
        table: String,
    ): List<String> {
        val indexes =
            database.query("PRAGMA index_list(`$table`)").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
            }
        return indexes.flatMap { index ->
            database.query("PRAGMA index_info(`$index`)").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
            }
        }
    }

    private companion object {
        const val TEST_DB = "manual-migration-test.db"

        // strftime('%s', '2019-05-01 10:00:00') and strftime('%s', '2019-05-01 10:05:00') (UTC)
        const val PLANTER_CREATED_EPOCH = 1_556_704_800L
        const val TREE_CREATED_EPOCH = 1_556_705_100L
        const val CHECK_IN_CREATED_AT = 1_556_704_900L

        val LEGACY_TABLES =
            listOf(
                "planter_details",
                "planter_identifications",
                "tree",
                "location",
                "photo",
                "tree_photo",
                "note",
                "tree_note",
                "tree_attributes",
                "pending_updates",
                "settings",
            )
    }
}