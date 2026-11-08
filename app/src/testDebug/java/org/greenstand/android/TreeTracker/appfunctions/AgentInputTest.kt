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

import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(Enclosed::class)
class AgentInputTest {
    @RunWith(Parameterized::class)
    class ValidNames(
        private val input: String,
        private val expected: String,
    ) {
        @Test
        fun `is accepted as first and last name`() {
            val details = AgentInput.accountDetails(firstName = input, lastName = input, phone = null, email = null)

            assertEquals(expected, details.firstName)
            assertEquals(expected, details.lastName)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>("Ada", "Ada"),
                    arrayOf<Any?>("Ng", "Ng"),
                    arrayOf<Any?>("  Ada  ", "Ada"),
                    arrayOf<Any?>("Mary Ann", "Mary Ann"),
                    arrayOf<Any?>("Mary-Jane", "Mary-Jane"),
                    arrayOf<Any?>("O'Brien", "O'Brien"),
                    arrayOf<Any?>("D'Angelo-Smith", "D'Angelo-Smith"),
                    arrayOf<Any?>("José", "José"),
                    arrayOf<Any?>("Zoë", "Zoë"),
                    arrayOf<Any?>("Łukasz", "Łukasz"),
                    arrayOf<Any?>("Ünal", "Ünal"),
                    arrayOf<Any?>("李雷", "李雷"),
                    arrayOf<Any?>("Мария", "Мария"),
                    arrayOf<Any?>("محمد", "محمد"),
                    arrayOf<Any?>("A".repeat(50), "A".repeat(50)),
                )
        }
    }

    @RunWith(Parameterized::class)
    class InvalidNames(
        private val input: String,
        private val expectedError: String,
    ) {
        @Test
        fun `is rejected with the sign-up rule it breaks`() {
            val error =
                assertFailsWith<IllegalArgumentException> {
                    AgentInput.accountDetails(firstName = input, lastName = null, phone = null, email = null)
                }

            assertTrue(error.message!!.startsWith("firstName "), error.message)
            assertTrue(error.message!!.endsWith(expectedError), error.message)
        }

        @Test
        fun `is rejected as a last name too`() {
            val error =
                assertFailsWith<IllegalArgumentException> {
                    AgentInput.accountDetails(firstName = null, lastName = input, phone = null, email = null)
                }

            assertTrue(error.message!!.startsWith("lastName "), error.message)
        }

        companion object {
            private const val EMPTY = "Name cannot be empty"
            private const val TOO_SHORT = "Name must be at least 2 characters"
            private const val TOO_LONG = "Name cannot exceed 50 characters"
            private const val SPECIAL = "Name cannot contain numbers or special characters"
            private const val NOT_LETTERS = "Name can only contain letters, spaces, hyphens and apostrophes"

            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>("", EMPTY),
                    arrayOf<Any?>("   ", EMPTY),
                    arrayOf<Any?>("A", TOO_SHORT),
                    arrayOf<Any?>(" A ", TOO_SHORT),
                    arrayOf<Any?>("A".repeat(51), TOO_LONG),
                    arrayOf<Any?>("Ada2", SPECIAL),
                    arrayOf<Any?>("Ada!", SPECIAL),
                    arrayOf<Any?>("Ada_Lovelace", SPECIAL),
                    arrayOf<Any?>("Ada.L", SPECIAL),
                    arrayOf<Any?>("Ada@home", SPECIAL),
                    arrayOf<Any?>("Ada \"The\" Count", SPECIAL),
                    arrayOf<Any?>("Ada😀", NOT_LETTERS),
                    arrayOf<Any?>("Ada–Lovelace", NOT_LETTERS),
                )
        }
    }

    @RunWith(Parameterized::class)
    class ValidPhones(
        private val input: String,
        private val expected: String,
    ) {
        @Test
        fun `is accepted`() {
            val details = AgentInput.accountDetails(firstName = null, lastName = null, phone = input, email = null)

            assertEquals(expected, details.phone)
            assertNull(details.email)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>("1234567", "1234567"),
                    arrayOf<Any?>("5551234567", "5551234567"),
                    arrayOf<Any?>("123456789012345", "123456789012345"),
                    arrayOf<Any?>(" 5551234567 ", "5551234567"),
                    arrayOf<Any?>("0000000", "0000000"),
                )
        }
    }

    /** Sign-up rejects these too: it only accepts 7 to 15 digits, with nothing else. */
    @RunWith(Parameterized::class)
    class InvalidPhones(
        private val input: String,
    ) {
        @Test
        fun `is rejected`() {
            val error =
                assertFailsWith<IllegalArgumentException> {
                    AgentInput.accountDetails(firstName = null, lastName = null, phone = input, email = null)
                }

            assertEquals("phone '${input.trim()}' must be 7 to 15 digits.", error.message)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>(""),
                    arrayOf<Any?>("   "),
                    arrayOf<Any?>("123456"),
                    arrayOf<Any?>("1234567890123456"),
                    arrayOf<Any?>("+15551234567"),
                    arrayOf<Any?>("555-123-4567"),
                    arrayOf<Any?>("555 123 4567"),
                    arrayOf<Any?>("(555)1234567"),
                    arrayOf<Any?>("555123456a"),
                    arrayOf<Any?>("phone"),
                    arrayOf<Any?>("５５５１２３４５６７"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class ValidEmails(
        private val input: String,
        private val expected: String,
    ) {
        @Test
        fun `is accepted and normalized like sign-up`() {
            val details = AgentInput.accountDetails(firstName = null, lastName = null, phone = null, email = input)

            assertEquals(expected, details.email)
            assertNull(details.phone)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>("ada@example.com", "ada@example.com"),
                    arrayOf<Any?>("Ada@Example.COM", "ada@example.com"),
                    arrayOf<Any?>("  ada@example.com  ", "ada@example.com"),
                    arrayOf<Any?>("qa+1@example.com", "qa+1@example.com"),
                    arrayOf<Any?>("first.last@sub.example.co.uk", "first.last@sub.example.co.uk"),
                    arrayOf<Any?>("x@y.travel", "x@y.travel"),
                    arrayOf<Any?>("a@b", "a@b"),
                    arrayOf<Any?>("@", "@"),
                    arrayOf<Any?>("ÄDA@BEISPIEL.DE", "äda@beispiel.de"),
                    arrayOf<Any?>("用户@例子.广告", "用户@例子.广告"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class InvalidEmails(
        private val input: String,
    ) {
        @Test
        fun `is rejected because it has no @`() {
            val error =
                assertFailsWith<IllegalArgumentException> {
                    AgentInput.accountDetails(firstName = null, lastName = null, phone = null, email = input)
                }

            assertEquals("email '${input.trim().lowercase()}' must contain an @.", error.message)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>(""),
                    arrayOf<Any?>("   "),
                    arrayOf<Any?>("plainaddress"),
                    arrayOf<Any?>("example.com"),
                    arrayOf<Any?>("ada(at)example.com"),
                    arrayOf<Any?>("ada＠example.com"),
                )
        }
    }

    class AccountCombinations {
        @Test
        fun `nothing passed gives the default names and no phone or email`() {
            val details = AgentInput.accountDetails(null, null, null, null)

            assertEquals(AgentInput.AccountDetails("Test", "User", null, null), details)
        }

        @Test
        fun `phone and email together are rejected, even when one is invalid`() {
            listOf(
                "5551234567" to "ada@example.com",
                "123" to "ada@example.com",
                "5551234567" to "not-an-email",
                "" to "",
            ).forEach { (phone, email) ->
                val error =
                    assertFailsWith<IllegalArgumentException>("$phone / $email") {
                        AgentInput.accountDetails(null, null, phone, email)
                    }
                assertEquals("Pass a phone or an email, not both.", error.message)
            }
        }

        @Test
        fun `names are checked before the phone`() {
            val error = assertFailsWith<IllegalArgumentException> { AgentInput.accountDetails("A", null, "123", null) }

            assertTrue(error.message!!.startsWith("firstName"), error.message)
        }
    }

    @RunWith(Parameterized::class)
    class WalletLookups(
        private val input: String,
        private val expected: List<String>,
    ) {
        @Test
        fun `tries the wallet as given, then lowercased`() {
            assertEquals(expected, AgentInput.walletLookups(input))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "\"{0}\"")
            fun cases() =
                listOf(
                    arrayOf<Any?>("5551234567", listOf("5551234567")),
                    arrayOf<Any?>("ada@example.com", listOf("ada@example.com")),
                    arrayOf<Any?>("Ada@Example.com", listOf("Ada@Example.com", "ada@example.com")),
                    arrayOf<Any?>(" ada@example.com ", listOf("ada@example.com")),
                    arrayOf<Any?>("Legacy Wallet", listOf("Legacy Wallet", "legacy wallet")),
                )
        }
    }

    @RunWith(Parameterized::class)
    class TreeCounts(
        private val count: Int,
        private val isValid: Boolean,
    ) {
        @Test
        fun `is checked against 1 to 500`() {
            if (isValid) {
                AgentInput.requireTreeCount(count)
            } else {
                val error = assertFailsWith<IllegalArgumentException> { AgentInput.requireTreeCount(count) }
                assertEquals("count must be between 1 and 500, was $count.", error.message)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0} valid={1}")
            fun cases() =
                listOf(
                    arrayOf<Any?>(1, true),
                    arrayOf<Any?>(2, true),
                    arrayOf<Any?>(250, true),
                    arrayOf<Any?>(499, true),
                    arrayOf<Any?>(500, true),
                    arrayOf<Any?>(0, false),
                    arrayOf<Any?>(-1, false),
                    arrayOf<Any?>(501, false),
                    arrayOf<Any?>(Int.MAX_VALUE, false),
                    arrayOf<Any?>(Int.MIN_VALUE, false),
                )
        }
    }

    @RunWith(Parameterized::class)
    class ValidTreeCenters(
        private val latitude: Double?,
        private val longitude: Double?,
    ) {
        @Test
        fun `is passed through unchanged`() {
            val center = AgentInput.treeCenter(latitude, longitude)

            if (latitude == null) {
                assertNull(center)
            } else {
                assertEquals<Pair<Double?, Double?>?>(latitude to longitude, center)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}, {1}")
            fun cases() =
                listOf(
                    arrayOf<Any?>(null, null),
                    arrayOf<Any?>(0.0, 0.0),
                    arrayOf<Any?>(-3.5, 37.3),
                    arrayOf<Any?>(37.422, -122.084),
                    arrayOf<Any?>(90.0, 180.0),
                    arrayOf<Any?>(-90.0, -180.0),
                    arrayOf<Any?>(-0.0, -0.0),
                )
        }
    }

    @RunWith(Parameterized::class)
    class InvalidTreeCenters(
        private val latitude: Double?,
        private val longitude: Double?,
        private val expectedError: String,
    ) {
        @Test
        fun `is rejected`() {
            val error = assertFailsWith<IllegalArgumentException> { AgentInput.treeCenter(latitude, longitude) }

            assertEquals(expectedError, error.message)
        }

        companion object {
            private const val PAIR = "Pass both latitude and longitude, or neither."

            @JvmStatic
            @Parameterized.Parameters(name = "{0}, {1}")
            fun cases() =
                listOf(
                    arrayOf<Any?>(10.0, null, PAIR),
                    arrayOf<Any?>(null, 10.0, PAIR),
                    arrayOf<Any?>(90.0001, 0.0, "latitude must be between -90 and 90, was 90.0001."),
                    arrayOf<Any?>(-90.0001, 0.0, "latitude must be between -90 and 90, was -90.0001."),
                    arrayOf<Any?>(0.0, 180.0001, "longitude must be between -180 and 180, was 180.0001."),
                    arrayOf<Any?>(0.0, -180.0001, "longitude must be between -180 and 180, was -180.0001."),
                    arrayOf<Any?>(Double.NaN, 0.0, "latitude must be between -90 and 90, was NaN."),
                    arrayOf<Any?>(0.0, Double.NaN, "longitude must be between -180 and 180, was NaN."),
                    arrayOf<Any?>(Double.POSITIVE_INFINITY, 0.0, "latitude must be between -90 and 90, was Infinity."),
                    arrayOf<Any?>(0.0, Double.NEGATIVE_INFINITY, "longitude must be between -180 and 180, was -Infinity."),
                )
        }
    }

    @RunWith(Parameterized::class)
    class NormalizedCoordinates(
        private val latitude: Double,
        private val longitude: Double,
        private val expectedLatitude: Double,
        private val expectedLongitude: Double,
    ) {
        @Test
        fun `is clamped and wrapped into range`() {
            val (normalizedLatitude, normalizedLongitude) = AgentInput.normalizeCoordinates(latitude, longitude)

            assertEquals(expectedLatitude, normalizedLatitude, 1e-9)
            assertEquals(expectedLongitude, normalizedLongitude, 1e-9)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}, {1}")
            fun cases() =
                listOf(
                    arrayOf<Any?>(0.0, 0.0, 0.0, 0.0),
                    arrayOf<Any?>(45.0, 179.5, 45.0, 179.5),
                    arrayOf<Any?>(45.0, -179.5, 45.0, -179.5),
                    arrayOf<Any?>(45.0, 180.0, 45.0, -180.0),
                    arrayOf<Any?>(45.0, -180.0, 45.0, -180.0),
                    arrayOf<Any?>(45.0, 180.5, 45.0, -179.5),
                    arrayOf<Any?>(45.0, -180.5, 45.0, 179.5),
                    arrayOf<Any?>(45.0, 360.0, 45.0, 0.0),
                    arrayOf<Any?>(45.0, 540.0, 45.0, -180.0),
                    arrayOf<Any?>(45.0, 725.0, 45.0, 5.0),
                    arrayOf<Any?>(45.0, -725.0, 45.0, -5.0),
                    arrayOf<Any?>(90.0, 10.0, 90.0, 10.0),
                    arrayOf<Any?>(90.5, 10.0, 90.0, 10.0),
                    arrayOf<Any?>(-95.0, 10.0, -90.0, 10.0),
                    arrayOf<Any?>(-90.0, 10.0, -90.0, 10.0),
                )
        }
    }

    class NormalizedCoordinatesSweep {
        @Test
        fun `any longitude lands in range at the same position on the globe`() {
            val random = Random(42)
            repeat(10_000) {
                val longitude = random.nextDouble(-1e6, 1e6)

                val (_, wrapped) = AgentInput.normalizeCoordinates(0.0, longitude)

                assertTrue(wrapped >= -180.0 && wrapped < 180.0, "$longitude -> $wrapped")
                val turns = (longitude - wrapped) / 360.0
                assertTrue(abs(turns - Math.round(turns)) < 1e-6, "$longitude -> $wrapped is not a whole turn away")
            }
        }

        @Test
        fun `huge longitudes from spreading at a pole stay in range`() {
            listOf(1e13, -1e13, 1.7e308, -1.7e308).forEach { longitude ->
                val (_, wrapped) = AgentInput.normalizeCoordinates(89.9999, longitude)

                assertTrue(wrapped >= -180.0 && wrapped < 180.0, "$longitude -> $wrapped")
            }
        }
    }
}