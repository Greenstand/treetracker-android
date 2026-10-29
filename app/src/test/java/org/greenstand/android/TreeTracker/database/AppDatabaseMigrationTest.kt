/*
 * Copyright 2023 Treetracker
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
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a database from each exported Room schema (app/schemas), then opens it with the
 * app's configuration (AppDatabase.builder). Opening runs the migrations, and Room validates every
 * migrated table against the current entities, so a missing or broken migration fails here.
 *
 * MigrationTestHelper isn't used because it reads schemas from instrumentation assets, which
 * Robolectric unit tests in this module don't have (includeAndroidResources is off).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun `schema 6 (the 1_4_0 release) upgrades to latest and keeps unsynced legacy trees`() =
        runTest {
            createDatabase(version = 6) { db ->
                db.execSQL(
                    "INSERT INTO planter_info (_id, planter_identifier, first_name, last_name, latitude, longitude, " +
                        "uploaded, created_at, record_uuid) VALUES (1, 'planter@example.com', 'Ada', 'Lovelace', 1.0, 2.0, " +
                        "0, 1600000000, 'record-uuid')",
                )
                db.execSQL(
                    "INSERT INTO planter_check_in (_id, planter_info_id, local_photo_path, latitude, longitude, created_at) " +
                        "VALUES (1, 1, '/selfie.jpg', 1.0, 2.0, 1600000000)",
                )
                db.execSQL(
                    "INSERT INTO tree_capture (_id, uuid, planter_checkin_id, local_photo_path, note_content, latitude, " +
                        "longitude, accuracy, uploaded, created_at) VALUES (1, 'tree-uuid', 1, '/tree.jpg', 'note', 1.0, " +
                        "2.0, 3.0, 0, 1600000000)",
                )
            }

            val database = openLatest()
            try {
                val dao = database.treeTrackerDao()

                assertEquals(listOf(1L), dao.getAllTreeCaptureIdsToUpload())
                assertEquals("/tree.jpg", dao.getTreeCaptureById(1L).localPhotoPath)
                assertEquals(0, dao.getNonUploadedTreeCount())
            } finally {
                database.close()
            }
        }

    @Test
    fun `schema 7 upgrades to latest and keeps legacy planters, captures, attributes and locations`() =
        runTest {
            createDatabase(version = 7) { db ->
                db.execSQL(
                    "INSERT INTO planter_info (_id, planter_identifier, first_name, last_name, latitude, longitude, " +
                        "uploaded, created_at, record_uuid) VALUES (1, 'planter@example.com', 'Ada', 'Lovelace', 1.0, 2.0, " +
                        "1, 1600000000, 'record-uuid')",
                )
                db.execSQL(
                    "INSERT INTO planter_check_in (_id, planter_info_id, local_photo_path, latitude, longitude, created_at) " +
                        "VALUES (1, 1, NULL, 1.0, 2.0, 1600000000)",
                )
                db.execSQL(
                    "INSERT INTO tree_capture (_id, uuid, planter_checkin_id, local_photo_path, photo_url, note_content, " +
                        "latitude, longitude, accuracy, uploaded, created_at) VALUES (1, 'tree-uuid', 1, NULL, " +
                        "'https://photo', 'note', 1.0, 2.0, 3.0, 1, 1600000000)",
                )
                db.execSQL("INSERT INTO tree_attribute (_id, `key`, `value`, tree_capture_id) VALUES (1, 'height', '4', 1)")
                db.execSQL("INSERT INTO location_data (_id, json_value, uploaded, created_at) VALUES (1, '{}', 0, 1600000000)")
            }

            val database = openLatest()
            try {
                val treeDao = database.treeDao()

                assertEquals(listOf("planter@example.com"), database.planterDao().getAllPlanterInfoList().map { it.identifier })
                assertEquals(1, treeDao.getUploadedTreeCaptureCount())
                assertEquals(1, treeDao.getUploadedLegacyTreeImageCount())
                assertEquals(listOf("height" to "4"), treeDao.getTreeAttributeByTreeCaptureId(1L).map { it.key to it.value })
                val legacyLocations =
                    database.openHelper.readableDatabase
                        .query("SELECT COUNT(*) FROM location_data")
                        .use { cursor ->
                            cursor.moveToFirst()
                            cursor.getInt(0)
                        }
                assertEquals(1, legacyLocations)
                assertTrue(database.userDao().getAllUsersList().isEmpty())
                assertTrue(database.organizationDao().getAllOrgs().isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `schema 8 (the 2_0_0 release) upgrades to latest and keeps users, sessions, trees and locations`() =
        runTest {
            createDatabase(version = 8) { db ->
                db.execSQL(
                    "INSERT INTO device_config (_id, uuid, app_version, app_build, os_version, sdk_version, logged_at, " +
                        "uploaded, bundle_id) VALUES (1, 'device-uuid', '2.0.0', 200, '13', 33, '2023-06-15T10:30:00Z', 0, NULL)",
                )
                db.execSQL(
                    "INSERT INTO user (_id, uuid, wallet, first_name, last_name, phone, email, latitude, longitude, uploaded, " +
                        "created_at, bundle_id, photo_path, photo_url, power_user) VALUES (1, 'user-uuid', 'wallet', 'Ada', " +
                        "'Lovelace', NULL, 'ada@example.com', 1.0, 2.0, 0, '2023-06-15T10:30:00Z', NULL, '/selfie.jpg', NULL, 1)",
                )
                db.execSQL(
                    "INSERT INTO session (_id, uuid, origin_user_id, origin_wallet, destination_wallet, start_time, end_time, " +
                        "organization, uploaded, bundle_id, device_config_id) VALUES (1, 'session-uuid', 'user-uuid', 'wallet', " +
                        "'wallet', '2023-06-15T10:30:00Z', NULL, 'org', 0, NULL, 1)",
                )
                db.execSQL(
                    "INSERT INTO tree (_id, uuid, session_id, photo_path, photo_url, note, latitude, longitude, uploaded, " +
                        "created_at, bundle_id, extra_attributes) VALUES (1, 'tree-uuid', 1, '/tree.jpg', NULL, 'note', 3.0, " +
                        "4.0, 0, '2023-06-15T10:31:00Z', NULL, '{\"height\":\"3\"}')",
                )
                db.execSQL("INSERT INTO location (_id, json_value, session_id, uploaded, create_at) VALUES (1, '{}', 1, 0, 1686825000000)")
            }

            val database = openLatest()
            try {
                val session = database.sessionDao().getSessionById(1L)
                assertEquals("session-uuid", session.uuid)
                assertEquals(1L, session.deviceConfigId)
                assertEquals(Instant.parse("2023-06-15T10:30:00Z"), session.startTime)
                assertNull(session.note)

                val user = database.userDao().getUserByWallet("wallet")
                assertEquals("ada@example.com", user?.email)
                assertEquals(true, user?.powerUser)

                val tree = database.treeDao().getTreesByIds(listOf(1L)).single()
                assertEquals("tree-uuid", tree.uuid)
                assertEquals(mapOf("height" to "3"), tree.extraAttributes)
                assertEquals(Instant.parse("2023-06-15T10:31:00Z"), tree.createdAt)

                assertEquals(listOf(1L), database.locationDao().getLocationsForTreeSessions().map { it.id })
                assertEquals("device-uuid", database.deviceConfigDao().getLatestDeviceConfig()?.uuid)
                assertTrue(database.organizationDao().getAllOrgs().isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `sessions migrated from schema 8 accept a note and orgs can be stored`() =
        runTest {
            createDatabase(version = 8) { db ->
                db.execSQL(
                    "INSERT INTO session (_id, uuid, origin_user_id, origin_wallet, destination_wallet, start_time, end_time, " +
                        "organization, uploaded, bundle_id, device_config_id) VALUES (1, 'session-uuid', 'user-uuid', 'wallet', " +
                        "'wallet', '2023-06-15T10:30:00Z', NULL, NULL, 0, NULL, NULL)",
                )
            }

            val database = openLatest()
            try {
                val sessionDao = database.sessionDao()
                sessionDao.updateSession(sessionDao.getSessionById(1L).copy(note = "added after upgrade").apply { id = 1L })
                database.organizationDao().insertOrg(DbFixtures.org(id = "org"))

                assertEquals("added after upgrade", sessionDao.getSessionById(1L).note)
                assertEquals("org", database.organizationDao().getOrg("org")?.id)
            } finally {
                database.close()
            }
        }

    @Test
    fun `every exported schema from version 6 upgrades to latest`() {
        val exported = exportedSchemaVersions()
        assertTrue("No exported schemas found in $SCHEMA_DIR", exported.isNotEmpty())
        // The current version's JSON is regenerated by every build, so opening it can't fail; skip it.
        val versions = exported.filter { it >= FIRST_MIGRATABLE_VERSION && it < exported.max() }
        assertTrue("No older schemas to upgrade from", versions.isNotEmpty())

        for (version in versions) {
            createDatabase(version)
            try {
                val database = openLatest()
                database.openHelper.writableDatabase
                database.close()
            } catch (e: Exception) {
                throw AssertionError("Upgrading from schema $version failed", e)
            } finally {
                context.deleteDatabase(TEST_DB)
            }
        }
    }

    private fun openLatest(): AppDatabase =
        AppDatabase
            .builder(context, TEST_DB)
            .allowMainThreadQueries()
            .build()

    /** Creates [TEST_DB] exactly as described by the exported schema for [version]. */
    private fun createDatabase(
        version: Int,
        seed: (SQLiteDatabase) -> Unit = {},
    ) {
        val schema =
            Json
                .parseToJsonElement(File(SCHEMA_DIR, "$version.json").readText())
                .jsonObject
                .getValue("database")
                .jsonObject
        val file = context.getDatabasePath(TEST_DB).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                db.execSQL(
                    entity
                        .getValue("createSql")
                        .jsonPrimitive.content
                        .replace("\${TABLE_NAME}", table),
                )
                entity["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(
                        index.jsonObject
                            .getValue("createSql")
                            .jsonPrimitive.content
                            .replace("\${TABLE_NAME}", table),
                    )
                }
            }
            schema["views"]?.jsonArray?.forEach { element ->
                val view = element.jsonObject
                val name = view.getValue("viewName").jsonPrimitive.content
                db.execSQL(
                    view
                        .getValue("createSql")
                        .jsonPrimitive.content
                        .replace("\${VIEW_NAME}", name),
                )
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            seed(db)
            db.version = version
        }
    }

    private fun exportedSchemaVersions(): List<Int> =
        File(SCHEMA_DIR)
            .listFiles()
            .orEmpty()
            .mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }
            .sorted()

    companion object {
        private const val TEST_DB = "migration-test.db"

        // Unit tests run with the app module as the working directory.
        private const val SCHEMA_DIR = "schemas/org.greenstand.android.TreeTracker.database.AppDatabase"

        // 1.json-4.json don't describe the databases that shipped: d820c63e rewrote MIGRATION_3_4/4_5 to match
        // production without re-exporting them (e.g. 3.json has planter_info, which MIGRATION_3_4 creates),
        // and 5.json is missing. 6.json onward match, so start there.
        private const val FIRST_MIGRATABLE_VERSION = 6
    }
}