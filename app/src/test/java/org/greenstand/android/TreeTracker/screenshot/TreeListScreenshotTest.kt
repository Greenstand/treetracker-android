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
package org.greenstand.android.TreeTracker.screenshot

import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.treeedit.TreeList
import org.greenstand.android.TreeTracker.treeedit.TreeListState
import org.junit.Test

class TreeListScreenshotTest : ScreenshotTest() {
    @Test
    fun treeList_empty() =
        snapshot {
            TreeList(
                state = TreeListState(isLoading = false),
                userName = "John Doe",
            )
        }

    @Test
    fun treeList_withTrees() =
        snapshot {
            TreeList(
                state =
                    TreeListState(
                        isLoading = false,
                        trees = fakeTrees(),
                    ),
                userName = "John Doe",
            )
        }

    @Test
    fun treeList_withSelectedTree() =
        snapshot {
            val trees = fakeTrees()
            TreeList(
                state =
                    TreeListState(
                        isLoading = false,
                        trees = trees,
                        selectedTree = trees.first(),
                    ),
                userName = "John Doe",
            )
        }

    private fun fakeTrees(): List<TreeEntity> =
        listOf(
            TreeEntity(
                uuid = "abc-123",
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = "Healthy oak tree",
                latitude = 12.345,
                longitude = -67.890,
                uploaded = false,
                createdAt = Instant.parse("2026-04-01T10:00:00Z"),
            ).apply { id = 1 },
            TreeEntity(
                uuid = "def-456",
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = "",
                latitude = 12.346,
                longitude = -67.891,
                uploaded = true,
                createdAt = Instant.parse("2026-04-02T14:30:00Z"),
            ).apply { id = 2 },
            TreeEntity(
                uuid = "ghi-789",
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = "Near the river bank, good soil conditions",
                latitude = 12.347,
                longitude = -67.892,
                uploaded = false,
                createdAt = Instant.parse("2026-04-03T09:15:00Z"),
            ).apply { id = 3 },
        )
}
