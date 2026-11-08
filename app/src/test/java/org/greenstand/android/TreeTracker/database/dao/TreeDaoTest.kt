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
package org.greenstand.android.TreeTracker.database.dao

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.valuesBeforeAndAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [TreeDAO] queries on the current `tree` table. Legacy `tree_capture` queries are in TreeDaoLegacyCaptureTest. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeDaoTest : InMemoryDatabaseTest() {
    private val dao: TreeDAO get() = database.treeDao()

    private suspend fun insertSession(
        wallet: String = "wallet",
        uuid: String = "session",
    ): Long = database.sessionDao().insertSession(DbFixtures.session(uuid = uuid, originWallet = wallet))

    @Test
    fun `insertTree stores every field and returns the generated id`() =
        runTest {
            val sessionId = insertSession()
            val tree =
                DbFixtures.tree(
                    sessionId = sessionId,
                    photoUrl = "https://photo",
                    bundleId = "bundle",
                    extraAttributes = mapOf("height" to "3", "species" to "oak"),
                )

            val id = dao.insertTree(tree)

            val stored = dao.getTreesByIds(listOf(id)).single()
            assertEquals(tree, stored)
            assertEquals(id, stored.id)
        }

    @Test
    fun `insertTree keeps null optional fields`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertTree(DbFixtures.tree(sessionId, photoPath = null, photoUrl = null, bundleId = null, extraAttributes = null))

            val stored = dao.getTreesByIds(listOf(id)).single()
            assertNull(stored.photoPath)
            assertNull(stored.photoUrl)
            assertNull(stored.bundleId)
            assertNull(stored.extraAttributes)
        }

    @Test
    fun `insertTree fails when the session does not exist`() =
        runTest {
            val result = runCatching { dao.insertTree(DbFixtures.tree(sessionId = 404)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getAllTrees().isEmpty())
        }

    @Test
    fun `insertTree fails for an id that already exists`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertTree(DbFixtures.tree(sessionId, uuid = "original"))

            val result = runCatching { dao.insertTree(DbFixtures.tree(sessionId, uuid = "duplicate").apply { this.id = id }) }

            assertTrue(result.isFailure)
            assertEquals(listOf("original"), dao.getAllTrees().map { it.uuid })
        }

    @Test
    fun `getAllTrees returns every tree`() =
        runTest {
            val sessionId = insertSession()
            dao.insertTree(DbFixtures.tree(sessionId, uuid = "a"))
            dao.insertTree(DbFixtures.tree(sessionId, uuid = "b"))

            assertEquals(setOf("a", "b"), dao.getAllTrees().map { it.uuid }.toSet())
        }

    @Test
    fun `getTreesByIds returns only the requested trees and skips unknown ids`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId, uuid = "a"))
            dao.insertTree(DbFixtures.tree(sessionId, uuid = "b"))
            val third = dao.insertTree(DbFixtures.tree(sessionId, uuid = "c"))

            assertEquals(setOf("a", "c"), dao.getTreesByIds(listOf(first, third, 999L)).map { it.uuid }.toSet())
        }

    @Test
    fun `getTreesByIds with no ids returns nothing`() =
        runTest {
            dao.insertTree(DbFixtures.tree(insertSession()))

            assertTrue(dao.getTreesByIds(emptyList()).isEmpty())
        }

    @Test
    fun `getTreesByUserWallet returns only trees from sessions of that wallet`() =
        runTest {
            val mine = insertSession(wallet = "mine", uuid = "s1")
            val theirs = insertSession(wallet = "theirs", uuid = "s2")
            dao.insertTree(DbFixtures.tree(mine, uuid = "my-tree"))
            dao.insertTree(DbFixtures.tree(theirs, uuid = "their-tree"))

            assertEquals(listOf("my-tree"), dao.getTreesByUserWallet("mine").first().map { it.uuid })
            assertTrue(dao.getTreesByUserWallet("nobody").first().isEmpty())
        }

    @Test
    fun `getTreesByUserWallet emits again when a tree is added for that wallet`() =
        runTest {
            val sessionId = insertSession(wallet = "mine")
            val tree = DbFixtures.tree(sessionId)

            val (before, after) = dao.getTreesByUserWallet("mine").valuesBeforeAndAfter { dao.insertTree(tree) }

            assertEquals(emptyList<TreeEntity>(), before)
            assertEquals(listOf(tree), after)
        }

    @Test
    fun `getTreeCountFromSessionId counts only trees of that session`() =
        runTest {
            val first = insertSession(uuid = "s1")
            val second = insertSession(uuid = "s2")
            dao.insertTree(DbFixtures.tree(first))
            dao.insertTree(DbFixtures.tree(first))
            dao.insertTree(DbFixtures.tree(second))

            assertEquals(2, dao.getTreeCountFromSessionId(first))
            assertEquals(1, dao.getTreeCountFromSessionId(second))
            assertEquals(0, dao.getTreeCountFromSessionId(999))
        }

    @Test
    fun `upload counts split trees by the uploaded flag`() =
        runTest {
            val sessionId = insertSession()
            dao.insertTree(DbFixtures.tree(sessionId, uploaded = true))
            dao.insertTree(DbFixtures.tree(sessionId, uploaded = false))
            dao.insertTree(DbFixtures.tree(sessionId, uploaded = false))

            assertEquals(1, dao.getUploadedTreeCount())
            assertEquals(2, dao.getNonUploadedTreeCount())
        }

    @Test
    fun `upload counts are zero for an empty table`() =
        runTest {
            assertEquals(0, dao.getUploadedTreeCount())
            assertEquals(0, dao.getNonUploadedTreeCount())
            assertEquals(0, dao.getUploadedTreeImageCount())
            assertEquals(0, dao.getNonUploadedTreeImageCount())
        }

    @Test
    fun `image counts split trees by whether they have a photo url`() =
        runTest {
            val sessionId = insertSession()
            dao.insertTree(DbFixtures.tree(sessionId, photoUrl = "https://a"))
            dao.insertTree(DbFixtures.tree(sessionId, photoUrl = "https://b"))
            dao.insertTree(DbFixtures.tree(sessionId, photoUrl = null))

            assertEquals(2, dao.getUploadedTreeImageCount())
            assertEquals(1, dao.getNonUploadedTreeImageCount())
            assertEquals(2, dao.getUploadedTreeImageCountFlow().first())
            assertEquals(1, dao.getNonUploadedTreeImageCountFlow().first())
        }

    @Test
    fun `image counts ignore the uploaded flag`() =
        runTest {
            val sessionId = insertSession()
            dao.insertTree(DbFixtures.tree(sessionId, photoUrl = null, uploaded = true))

            assertEquals(1, dao.getNonUploadedTreeImageCount())
            assertEquals(0, dao.getUploadedTreeImageCount())
        }

    @Test
    fun `getNonUploadedTreeImageCountFlow emits again when a tree without a photo url is added`() =
        runTest {
            val sessionId = insertSession()

            val (before, after) =
                dao.getNonUploadedTreeImageCountFlow().valuesBeforeAndAfter {
                    dao.insertTree(DbFixtures.tree(sessionId, photoUrl = null))
                }

            assertEquals(0, before)
            assertEquals(1, after)
        }

    @Test
    fun `getUploadedTreeImageCountFlow emits again when a tree gets a photo url`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertTree(DbFixtures.tree(sessionId, photoUrl = null))
            val uploaded = DbFixtures.tree(sessionId, photoUrl = "https://photo").apply { this.id = id }

            val (before, after) = dao.getUploadedTreeImageCountFlow().valuesBeforeAndAfter { dao.updateTree(uploaded) }

            assertEquals(0, before)
            assertEquals(1, after)
        }

    @Test
    fun `getAllTreeIdsToUpload returns ids of trees that are not uploaded`() =
        runTest {
            val sessionId = insertSession()
            val pending = dao.insertTree(DbFixtures.tree(sessionId, uploaded = false))
            dao.insertTree(DbFixtures.tree(sessionId, uploaded = true))

            assertEquals(listOf(pending), dao.getAllTreeIdsToUpload())
        }

    @Test
    fun `deleteTreeById deletes only that tree`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId, uuid = "a"))
            dao.insertTree(DbFixtures.tree(sessionId, uuid = "b"))

            dao.deleteTreeById(first)

            assertEquals(listOf("b"), dao.getAllTrees().map { it.uuid })
        }

    @Test
    fun `deleteTreeById ignores an unknown id`() =
        runTest {
            dao.insertTree(DbFixtures.tree(insertSession()))

            dao.deleteTreeById(999)

            assertEquals(1, dao.getAllTrees().size)
        }

    @Test
    fun `updateTreesBundleIds sets the bundle id only on the given trees`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId))
            val second = dao.insertTree(DbFixtures.tree(sessionId))

            dao.updateTreesBundleIds(listOf(first), "bundle-1")

            assertEquals("bundle-1", dao.getTreesByIds(listOf(first)).single().bundleId)
            assertNull(dao.getTreesByIds(listOf(second)).single().bundleId)
        }

    @Test
    fun `updateTreesUploadStatus marks the given trees as uploaded`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId))
            val second = dao.insertTree(DbFixtures.tree(sessionId))

            dao.updateTreesUploadStatus(listOf(first), true)

            assertTrue(dao.getTreesByIds(listOf(first)).single().uploaded)
            assertFalse(dao.getTreesByIds(listOf(second)).single().uploaded)
            assertEquals(listOf(second), dao.getAllTreeIdsToUpload())
        }

    @Test
    fun `updateTreesUploadStatus can reset trees to not uploaded`() =
        runTest {
            val id = dao.insertTree(DbFixtures.tree(insertSession(), uploaded = true))

            dao.updateTreesUploadStatus(listOf(id), false)

            assertEquals(listOf(id), dao.getAllTreeIdsToUpload())
        }

    @Test
    fun `removeTreesLocalImagePaths clears the photo path only for the given trees`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId, photoPath = "/a.jpg", photoUrl = "https://a"))
            val second = dao.insertTree(DbFixtures.tree(sessionId, photoPath = "/b.jpg"))

            dao.removeTreesLocalImagePaths(listOf(first))

            val firstTree = dao.getTreesByIds(listOf(first)).single()
            assertNull(firstTree.photoPath)
            assertEquals("https://a", firstTree.photoUrl)
            assertEquals("/b.jpg", dao.getTreesByIds(listOf(second)).single().photoPath)
        }

    @Test
    fun `updateTree updates the row with the same id`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertTree(DbFixtures.tree(sessionId))
            val updated =
                DbFixtures
                    .tree(sessionId, photoUrl = "https://photo", extraAttributes = mapOf("dbh" to "12"))
                    .copy(note = "edited")
                    .apply { this.id = id }

            dao.updateTree(updated)

            assertEquals(updated, dao.getTreesByIds(listOf(id)).single())
        }
}