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

import org.greenstand.android.TreeTracker.utilities.Validation
import org.greenstand.android.TreeTracker.utils.ValidationUtils

/**
 * Checks and normalizes what agents pass to the AppFunctions, by the same rules as the sign-up
 * and capture screens. Bad input throws [IllegalArgumentException] with a message for the agent.
 */
object AgentInput {
    const val DEFAULT_FIRST_NAME = "Test"
    const val DEFAULT_LAST_NAME = "User"
    const val MAX_TREES = 500

    /** createAccount's input, ready to save. At most one of [phone] and [email] is set. */
    data class AccountDetails(
        val firstName: String,
        val lastName: String,
        val phone: String?,
        val email: String?,
    )

    fun accountDetails(
        firstName: String?,
        lastName: String?,
        phone: String?,
        email: String?,
    ): AccountDetails {
        require(phone == null || email == null) { "Pass a phone or an email, not both." }

        val first = firstName?.trim() ?: DEFAULT_FIRST_NAME
        val last = lastName?.trim() ?: DEFAULT_LAST_NAME
        requireValidName("firstName", first)
        requireValidName("lastName", last)

        val trimmedPhone = phone?.trim()
        trimmedPhone?.let { require(Validation.isValidPhoneNumber(it)) { "phone '$it' must be 7 to 15 digits." } }

        // Sign-up lowercases emails before they become the wallet, and only checks for an @
        val normalizedEmail = email?.trim()?.lowercase()
        normalizedEmail?.let { require('@' in it) { "email '$it' must contain an @." } }

        return AccountDetails(first, last, trimmedPhone, normalizedEmail)
    }

    /** Wallets to look an account up by: as given, then lowercased, as email wallets are saved. */
    fun walletLookups(wallet: String): List<String> = listOf(wallet.trim(), wallet.trim().lowercase()).distinct()

    fun requireTreeCount(count: Int) {
        require(count in 1..MAX_TREES) { "count must be between 1 and $MAX_TREES, was $count." }
    }

    /** The latitude and longitude to place trees around, or null when neither was passed. */
    fun treeCenter(
        latitude: Double?,
        longitude: Double?,
    ): Pair<Double, Double>? {
        require((latitude == null) == (longitude == null)) { "Pass both latitude and longitude, or neither." }
        if (latitude == null || longitude == null) return null
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90, was $latitude." }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180, was $longitude." }
        return latitude to longitude
    }

    /**
     * Brings a point back into range: latitude clamped to -90..90, longitude wrapped into
     * -180..180 (exclusive of 180). Spreading trees near a pole or the antimeridian can leave it.
     */
    fun normalizeCoordinates(
        latitude: Double,
        longitude: Double,
    ): Pair<Double, Double> {
        val wrappedLongitude = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return latitude.coerceIn(-90.0, 90.0) to wrappedLongitude
    }

    private fun requireValidName(
        field: String,
        name: String,
    ) {
        val (isValid, error) = ValidationUtils.validateName(name)
        require(isValid) { "$field '$name' is invalid: $error" }
    }
}