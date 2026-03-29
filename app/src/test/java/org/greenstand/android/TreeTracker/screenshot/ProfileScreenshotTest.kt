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

import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.profile.Profile
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.junit.Test

class ProfileScreenshotTest : ScreenshotTest() {

    private val sampleUser = User(
        id = 1L,
        wallet = "jane@example.com",
        numberOfTrees = 42,
        firstName = "Jane",
        lastName = "Planter",
        photoPath = "",
        isPowerUser = true,
        unreadMessagesAvailable = false,
    )

    @Test
    fun profile_loading() = snapshot {
        Profile(state = UserSelectState())
    }

    @Test
    fun profile_with_user() = snapshot {
        Profile(
            state = UserSelectState(
                selectedUser = sampleUser,
            ),
        )
    }

    @Test
    fun profile_edit_mode() = snapshot {
        Profile(
            state = UserSelectState(
                selectedUser = sampleUser,
                editMode = true,
            ),
        )
    }
}
