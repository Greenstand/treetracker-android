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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SessionTrackerTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var exceptionDataCollector: ExceptionDataCollector

    private lateinit var sessionTracker: SessionTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        val mockEditor = mockk<Preferences.Editor>(relaxed = true)
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor

        sessionTracker = SessionTracker(
            dao = dao,
            treesToSyncHelper = treesToSyncHelper,
            preferences = preferences,
            timeProvider = timeProvider,
            orgRepo = orgRepo,
            exceptionDataCollector = exceptionDataCollector,
        )
    }

    @Test
    fun `WHEN no active session THEN endSession no-ops`() = runTest {
        sessionTracker.endSession()

        coVerify(exactly = 0) { dao.getSessionById(any()) }
        coVerify(exactly = 0) { dao.updateSession(any()) }
    }

    @Test
    fun `WHEN no currentSessionId but prefs has valid value THEN wasSessionInterrupted returns true`() = runTest {
        every { preferences.getLong(any(), any()) } returns 5L

        val result = sessionTracker.wasSessionInterrupted()

        assertTrue(result)
    }

    @Test
    fun `WHEN prefs returns negative one THEN wasSessionInterrupted returns false`() = runTest {
        every { preferences.getLong(any(), any()) } returns -1L

        val result = sessionTracker.wasSessionInterrupted()

        assertFalse(result)
    }
}
