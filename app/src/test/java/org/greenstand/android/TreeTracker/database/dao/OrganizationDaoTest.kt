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

import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OrganizationDaoTest : InMemoryDatabaseTest() {
    private val dao: OrganizationDAO get() = database.organizationDao()

    @Test
    fun `insertOrg stores every field and getOrg finds it by id`() =
        runTest {
            val org = DbFixtures.org()

            dao.insertOrg(org)

            assertEquals(org, dao.getOrg("org-id"))
        }

    @Test
    fun `getOrg returns null for an unknown id`() =
        runTest {
            dao.insertOrg(DbFixtures.org(id = "org-id"))

            assertNull(dao.getOrg("other"))
        }

    @Test
    fun `getOrg returns null for a null id`() =
        runTest {
            dao.insertOrg(DbFixtures.org())

            assertNull(dao.getOrg(null))
        }

    @Test
    fun `insertOrg replaces an org with the same id`() =
        runTest {
            dao.insertOrg(DbFixtures.org(id = "org-id", name = "Old", version = 1))

            dao.insertOrg(DbFixtures.org(id = "org-id", name = "New", version = 2))

            val orgs = dao.getAllOrgs()
            assertEquals(1, orgs.size)
            assertEquals("New", orgs.single().name)
            assertEquals(2, orgs.single().version)
        }

    @Test
    fun `getAllOrgs returns every org`() =
        runTest {
            dao.insertOrg(DbFixtures.org(id = "a"))
            dao.insertOrg(DbFixtures.org(id = "b"))

            assertEquals(setOf("a", "b"), dao.getAllOrgs().map { it.id }.toSet())
        }

    @Test
    fun `getAllOrgs is empty when there are no orgs`() =
        runTest {
            assertTrue(dao.getAllOrgs().isEmpty())
        }
}