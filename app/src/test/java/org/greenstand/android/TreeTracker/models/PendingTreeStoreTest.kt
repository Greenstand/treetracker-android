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
package org.greenstand.android.TreeTracker.models

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PendingTreeStoreTest {
    private lateinit var preferences: Preferences
    private lateinit var store: PendingTreeStore

    private val tree =
        Tree(
            treeUuid = UUID.randomUUID(),
            sessionId = 7L,
            content = "a note",
            photoPath = "/data/photos/tree.jpg",
            meanLongitude = 36.8,
            meanLatitude = -1.3,
        ).apply {
            addTreeAttribute(Tree.ABS_STEP_COUNT_KEY, "120")
            addTreeAttribute(Tree.DELTA_STEP_COUNT_KEY, "5")
        }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = Preferences(context.getSharedPreferences("pending-tree-test", Context.MODE_PRIVATE))
        preferences.edit().remove(PendingTreeStore.PENDING_TREE_KEY).commit()
        store = PendingTreeStore(preferences)
    }

    @Test
    fun `load is null when nothing was saved`() {
        assertNull(store.load())
    }

    @Test
    fun `a new store reads back the saved tree, as after process death`() {
        store.save(tree)

        assertEquals(tree, PendingTreeStore(preferences).load())
    }

    @Test
    fun `clear forgets the tree`() {
        store.save(tree)

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun `unreadable data is dropped instead of crashing`() {
        preferences.edit().putString(PendingTreeStore.PENDING_TREE_KEY, "{not json").commit()

        assertNull(store.load())
        assertNull(preferences.getString(PendingTreeStore.PENDING_TREE_KEY))
    }
}