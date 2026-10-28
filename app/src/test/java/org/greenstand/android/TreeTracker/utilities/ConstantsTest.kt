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
package org.greenstand.android.TreeTracker.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantsTest {
    @Test
    fun `tree tracker url points at the web map`() {
        assertEquals("https://map.treetracker.org/", Constants.TREE_TRACKER_URL)
    }

    @Test
    fun `http 404 message matches the retrofit error text`() {
        assertEquals("HTTP 404 Not Found", Constants.LOCAL_MSG_ERROR_HTTP404)
    }
}