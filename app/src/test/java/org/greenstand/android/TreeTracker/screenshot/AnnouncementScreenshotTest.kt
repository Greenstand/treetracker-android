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
package org.greenstand.android.TreeTracker.screenshot

import org.greenstand.android.TreeTracker.messages.announcementmessage.Announcement
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementState
import org.junit.Test

class AnnouncementScreenshotTest : ScreenshotTest() {
    @Test
    fun announcement_default() =
        snapshot {
            Announcement(state = AnnouncementState())
        }

    @Test
    fun announcement_with_content() =
        snapshot {
            Announcement(
                state =
                    AnnouncementState(
                        from = "Greenstand Admin",
                        currentBody = "Thank you for your contributions to reforestation. Your trees are making a difference!",
                        currentTitle = "Monthly Update",
                    ),
            )
        }

    @Test
    fun announcement_with_url() =
        snapshot {
            Announcement(
                state =
                    AnnouncementState(
                        from = "Greenstand",
                        currentTitle = "New Feature Available",
                        currentBody = "We have launched a new mapping tool. Check it out!",
                        currentUrl = "https://map.treetracker.org",
                    ),
            )
        }
}