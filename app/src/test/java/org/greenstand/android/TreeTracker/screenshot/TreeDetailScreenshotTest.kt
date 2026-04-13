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
import org.greenstand.android.TreeTracker.treeedit.TreeDetail
import org.greenstand.android.TreeTracker.treeedit.TreeDetailState
import org.junit.Test

class TreeDetailScreenshotTest : ScreenshotTest() {
    @Test
    fun treeDetail_nonUploaded() =
        snapshot {
            TreeDetail(
                state =
                    TreeDetailState(
                        tree = fakeTree(uploaded = false),
                        editedNote = "Healthy oak tree",
                    ),
            )
        }

    @Test
    fun treeDetail_uploaded() =
        snapshot {
            TreeDetail(
                state =
                    TreeDetailState(
                        tree = fakeTree(uploaded = true),
                        editedNote = "Healthy oak tree",
                    ),
            )
        }

    private fun fakeTree(uploaded: Boolean) =
        TreeEntity(
            uuid = "abc-123-def-456",
            sessionId = 1,
            photoPath = null,
            photoUrl = null,
            note = "Healthy oak tree",
            latitude = 12.345678,
            longitude = -67.890123,
            uploaded = uploaded,
            createdAt = Instant.parse("2026-04-01T10:00:00Z"),
        ).apply { id = 1 }
}
