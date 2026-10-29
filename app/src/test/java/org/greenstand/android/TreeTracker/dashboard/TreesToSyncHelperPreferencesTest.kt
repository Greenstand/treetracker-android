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
package org.greenstand.android.TreeTracker.dashboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Runs [TreesToSyncHelper] against real [Preferences] backed by Robolectric shared preferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreesToSyncHelperPreferencesTest {
    private val dao = mockk<TreeTrackerDAO>()

    private lateinit var preferences: Preferences
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @Before
    fun setUp() {
        val sharedPreferences =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences("trees-to-sync-test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        preferences = Preferences(sharedPreferences)
        treesToSyncHelper = TreesToSyncHelper(preferences = preferences, dao = dao)
    }

    @Test
    fun `tree count is unknown before the first refresh`() {
        assertEquals(-1, treesToSyncHelper.getTreeCountToSync())
    }

    @Test
    fun `refreshed tree count can be read back`() =
        runTest {
            coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 4
            coEvery { dao.getNonUploadedTreeImageCount() } returns 6

            treesToSyncHelper.refreshTreeCountToSync()

            assertEquals(10, treesToSyncHelper.getTreeCountToSync())
        }

    @Test
    fun `refreshing again replaces the stored count`() =
        runTest {
            coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returnsMany listOf(4, 0)
            coEvery { dao.getNonUploadedTreeImageCount() } returnsMany listOf(6, 1)

            treesToSyncHelper.refreshTreeCountToSync()
            treesToSyncHelper.refreshTreeCountToSync()

            assertEquals(1, treesToSyncHelper.getTreeCountToSync())
        }
}