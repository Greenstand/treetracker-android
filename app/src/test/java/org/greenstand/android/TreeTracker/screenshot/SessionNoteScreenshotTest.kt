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

import org.greenstand.android.TreeTracker.sessionnote.SessionNote
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteState
import org.junit.Test

class SessionNoteScreenshotTest : ScreenshotTest() {

    @Test
    fun sessionNote_default() = snapshot {
        SessionNote(state = SessionNoteState())
    }

    @Test
    fun sessionNote_with_text() = snapshot {
        SessionNote(
            state = SessionNoteState(
                note = "Planted 15 mango trees near the river bank. Soil was moist and weather was favorable.",
            ),
        )
    }
}
