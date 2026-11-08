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
package org.greenstand.android.TreeTracker.models.captureflowdata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureFlowDataTest {
    @Test
    fun `defaults to no photo and no note`() {
        val data = CaptureFlowData()

        assertNull(data.treePhoto)
        assertNull(data.note)
    }

    @Test
    fun `copy replaces only the given field`() {
        val data = CaptureFlowData(treePhoto = "/trees/1.jpg", note = "young oak")

        val updated = data.copy(note = "tall oak")

        assertEquals("/trees/1.jpg", updated.treePhoto)
        assertEquals("tall oak", updated.note)
    }

    @Test
    fun `instances with the same values are equal`() {
        assertEquals(CaptureFlowData("/trees/1.jpg", "note"), CaptureFlowData("/trees/1.jpg", "note"))
        assertNotEquals(CaptureFlowData("/trees/1.jpg", "note"), CaptureFlowData("/trees/2.jpg", "note"))
    }
}