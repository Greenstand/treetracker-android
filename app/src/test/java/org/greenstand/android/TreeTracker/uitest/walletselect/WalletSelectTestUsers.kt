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
package org.greenstand.android.TreeTracker.uitest.walletselect

import org.greenstand.android.TreeTracker.models.user.User

internal const val CURRENT_USER_NAME = "Cara Mwangi"
internal const val OTHER_USER_NAME = "Dan Otieno"

/** The planter running the capture setup flow. */
internal val walletTestCurrentUser =
    User(
        id = 10,
        wallet = "cara-wallet",
        numberOfTrees = 5,
        firstName = "Cara",
        lastName = "Mwangi",
        photoPath = "/no/such/cara.jpg",
        isPowerUser = true,
        unreadMessagesAvailable = false,
    )

/** Another planter whose wallet trees can be planted for. */
internal val walletTestOtherUser =
    User(
        id = 20,
        wallet = "dan-wallet",
        numberOfTrees = 8,
        firstName = "Dan",
        lastName = "Otieno",
        photoPath = "/no/such/dan.jpg",
        isPowerUser = false,
        unreadMessagesAvailable = false,
    )