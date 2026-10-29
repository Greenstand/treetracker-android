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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TreeTest {
    private fun tree() =
        Tree(
            treeUuid = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"),
            sessionId = 7L,
            content = "",
            photoPath = "/photos/tree.jpg",
            meanLongitude = -122.0,
            meanLatitude = 37.0,
        )

    @Test
    fun `new tree has no capture attributes`() {
        assertTrue(tree().treeCaptureAttributes().isEmpty())
    }

    @Test
    fun `addTreeAttribute stores the value under the key`() {
        val tree = tree()

        tree.addTreeAttribute(Tree.DBH_ATTR_KEY, "12.5")

        assertEquals(mapOf(Tree.DBH_ATTR_KEY to "12.5"), tree.treeCaptureAttributes())
    }

    @Test
    fun `addTreeAttribute with an existing key replaces the value`() {
        val tree = tree()
        tree.addTreeAttribute(Tree.TREE_COLOR_ATTR_KEY, "green")

        tree.addTreeAttribute(Tree.TREE_COLOR_ATTR_KEY, "red")

        assertEquals("red", tree.treeCaptureAttributes()[Tree.TREE_COLOR_ATTR_KEY])
        assertEquals(1, tree.treeCaptureAttributes().size)
    }

    @Test
    fun `copy keeps the capture attributes`() {
        val tree = tree()
        tree.addTreeAttribute(Tree.APP_BUILD_ATTR_KEY, "100")

        val copy = tree.copy(content = "note")

        assertEquals("note", copy.content)
        assertEquals("100", copy.treeCaptureAttributes()[Tree.APP_BUILD_ATTR_KEY])
    }

    @Test
    fun `attribute keys match the values the server expects`() {
        assertEquals("height_color", Tree.TREE_COLOR_ATTR_KEY)
        assertEquals("app_build", Tree.APP_BUILD_ATTR_KEY)
        assertEquals("app_flavor", Tree.APP_FLAVOR_ATTR_KEY)
        assertEquals("app_version", Tree.APP_VERSION_ATTR_KEY)
        assertEquals("abs_step_count", Tree.ABS_STEP_COUNT_KEY)
        assertEquals("delta_step_count", Tree.DELTA_STEP_COUNT_KEY)
        assertEquals("rotation_matrix", Tree.ROTATION_MATRIX_KEY)
        assertEquals("dbh", Tree.DBH_ATTR_KEY)
    }
}