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
package org.greenstand.android.TreeTracker.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class TreesToSyncHelperTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        val mockEditor = mockk<Preferences.Editor>(relaxed = true)
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor

        treesToSyncHelper =
            TreesToSyncHelper(
                preferences = preferences,
                dao = dao,
            )
    }

    @Test
    fun `WHEN refreshTreeCountToSync called THEN sums legacy and new tree counts and stores in prefs`() =
        runTest {
            coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 5
            coEvery { dao.getNonUploadedTreeImageCount() } returns 10

            treesToSyncHelper.refreshTreeCountToSync()

            val mockEditor = preferences.edit()
            verify { mockEditor.putInt(any(), 15) }
        }

    @Test
    fun `WHEN getTreeCountToSync called THEN reads from prefs`() =
        runTest {
            every { preferences.getInt(any(), -1) } returns 42

            val result = treesToSyncHelper.getTreeCountToSync()

            assertEquals(42, result)
        }

    @Test
    fun `WHEN getTreeCountToSync called with no stored value THEN returns negative one as default`() =
        runTest {
            every { preferences.getInt(any(), -1) } returns -1

            val result = treesToSyncHelper.getTreeCountToSync()

            assertEquals(-1, result)
        }
}