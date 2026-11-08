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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    @Test
    fun `valid email addresses pass`() {
        listOf(
            "john@example.com",
            "john.doe@mail.example.org",
            "first-last@example.co.uk",
            "user_1@example.io",
            "user@192.168.1.1",
        ).forEach { assertTrue("$it should be valid", Validation.isEmailValid(it)) }
    }

    @Test
    fun `invalid email addresses fail`() {
        listOf(
            "",
            "plainaddress",
            "@example.com",
            "user@",
            "user@example",
            "user name@example.com",
            "user@example.c",
            "user@@example.com",
        ).forEach { assertFalse("$it should be invalid", Validation.isEmailValid(it)) }
    }

    @Test
    fun `phone numbers of 7 to 15 digits are valid`() {
        listOf("1234567", "0712345678", "254712345678", "123456789012345").forEach {
            assertTrue("$it should be valid", Validation.isValidPhoneNumber(it))
        }
    }

    @Test
    fun `phone numbers that are too short or too long are invalid`() {
        assertFalse(Validation.isValidPhoneNumber(""))
        assertFalse(Validation.isValidPhoneNumber("123456"))
        assertFalse(Validation.isValidPhoneNumber("1234567890123456"))
    }

    @Test
    fun `phone numbers with letters are invalid`() {
        assertFalse(Validation.isValidPhoneNumber("12345ab"))
        assertFalse(Validation.isValidPhoneNumber("phone-number"))
    }

    @Test
    fun `cleanPhoneNumber keeps a plain digit string as is`() {
        assertTrue(Validation.cleanPhoneNumber("0712345678") == "0712345678")
    }
}