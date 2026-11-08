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
package org.greenstand.android.TreeTracker.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticEventsTest {
    @Test
    fun `event names match the names reported to firebase`() {
        assertEquals("user_checkin", AnalyticEvents.USER_CHECK_IN)
        assertEquals("user_info_created", AnalyticEvents.USER_INFO_CREATED)
        assertEquals("tree_planted", AnalyticEvents.TREE_PLANTED)
        assertEquals("marker_clicked", AnalyticEvents.MARKER_CLICKED)
        assertEquals("stop_button_clicked", AnalyticEvents.STOP_BUTTON_CLICKED)
        assertEquals("sync_button_clicked", AnalyticEvents.SYNC_BUTTON_CLICKED)
        assertEquals("tree_note_added", AnalyticEvents.NOTE_ADDED)
        assertEquals("tree_color_added", AnalyticEvents.TREE_COLOR_ADDED)
        assertEquals("user_enter_email_phone", AnalyticEvents.USER_ENTERED_EMAIL_PHONE)
        assertEquals("user_entered_details", AnalyticEvents.USER_ENTERED_DETAILS)
    }

    @Test
    fun `event names are unique`() {
        val names =
            listOf(
                AnalyticEvents.USER_CHECK_IN,
                AnalyticEvents.USER_INFO_CREATED,
                AnalyticEvents.TREE_PLANTED,
                AnalyticEvents.MARKER_CLICKED,
                AnalyticEvents.STOP_BUTTON_CLICKED,
                AnalyticEvents.SYNC_BUTTON_CLICKED,
                AnalyticEvents.NOTE_ADDED,
                AnalyticEvents.TREE_COLOR_ADDED,
                AnalyticEvents.USER_ENTERED_EMAIL_PHONE,
                AnalyticEvents.USER_ENTERED_DETAILS,
            )

        assertEquals(names.size, names.toSet().size)
    }
}