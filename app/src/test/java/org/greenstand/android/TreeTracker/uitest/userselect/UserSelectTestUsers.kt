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
package org.greenstand.android.TreeTracker.uitest.userselect

import org.greenstand.android.TreeTracker.models.user.User

internal const val ANN_NAME = "Ann Lee"
internal const val BEN_NAME = "Ben Okoro"

/** Two planters with distinct names, wallets and tree counts so each can be found by text. */
internal val userSelectTestUsers =
    listOf(
        User(
            id = 1,
            wallet = "ann@example.org",
            numberOfTrees = 12,
            firstName = "Ann",
            lastName = "Lee",
            photoPath = "/no/such/ann.jpg",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        ),
        User(
            id = 2,
            wallet = "+254700000002",
            numberOfTrees = 34,
            firstName = "Ben",
            lastName = "Okoro",
            photoPath = "/no/such/ben.jpg",
            isPowerUser = false,
            unreadMessagesAvailable = true,
        ),
    )