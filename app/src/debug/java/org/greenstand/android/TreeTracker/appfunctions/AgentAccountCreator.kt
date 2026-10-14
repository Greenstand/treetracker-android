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
package org.greenstand.android.TreeTracker.appfunctions

import android.content.Context
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import kotlin.random.Random

/**
 * Creates accounts the way the sign-up flow does, with the bundled test image as the selfie.
 *
 * Bad input throws [IllegalArgumentException] with a message meant for the calling agent.
 */
class AgentAccountCreator(
    private val context: Context,
    private val userRepo: UserRepo,
    private val random: Random = Random.Default,
) {
    suspend fun createAccount(
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?,
    ): AgentAccount {
        val details = AgentInput.accountDetails(firstName, lastName, phone, email)

        val wallet = details.email ?: details.phone ?: generateUnusedPhone()
        require(!userRepo.doesUserExists(wallet)) { "An account with wallet '$wallet' already exists." }

        val isPowerUser = userRepo.getPowerUser() == null
        val userId =
            userRepo.createUser(
                firstName = details.firstName,
                lastName = details.lastName,
                phone = if (details.email == null) wallet else null,
                email = details.email,
                wallet = wallet,
                photoPath = ImageUtils.createTestImageFile(context).absolutePath,
                isPowerUser = isPowerUser,
            )

        return AgentAccount(
            userId = userId,
            wallet = wallet,
            firstName = details.firstName,
            lastName = details.lastName,
            isPowerUser = isPowerUser,
        )
    }

    private suspend fun generateUnusedPhone(): String {
        repeat(MAX_PHONE_ATTEMPTS) {
            val phone = "555" + random.nextInt(1_000_000, 10_000_000)
            if (!userRepo.doesUserExists(phone)) return phone
        }
        error("Could not generate an unused phone number. Pass phone or email instead.")
    }

    companion object {
        private const val MAX_PHONE_ATTEMPTS = 10
    }
}