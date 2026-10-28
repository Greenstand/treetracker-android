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
package org.greenstand.android.TreeTracker.utils

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {
    @Test
    fun `valid names should pass validation`() {
        val validNames =
            listOf(
                "John",
                "Mary-Jane",
                "O'Connor",
                "José María",
                "李明",
                "François",
            )

        validNames.forEach { name ->
            val (isValid, error) = ValidationUtils.validateName(name)
            assertTrue("$name should be valid", isValid)
            assertNull("$name should have no error", error)
        }
    }

    @Test
    fun `names with numbers should fail`() {
        val invalidNames = listOf("John123", "4ever", "Test1")

        invalidNames.forEach { name ->
            val (isValid, error) = ValidationUtils.validateName(name)
            assertFalse("$name should be invalid", isValid)
            assertEquals("Name cannot contain numbers or special characters", error)
        }
    }

    @Test
    fun `names with special characters should fail`() {
        val invalidNames = listOf("John@Doe", "Test!", "Name#1")

        invalidNames.forEach { name ->
            val (isValid, error) = ValidationUtils.validateName(name)
            assertFalse("$name should be invalid", isValid)
            assertNotNull(error)
        }
    }

    @Test
    fun `empty name should fail`() {
        val (isValid, error) = ValidationUtils.validateName("")
        assertFalse(isValid)
        assertEquals("Name cannot be empty", error)
    }

    @Test
    fun `filterNameInput removes invalid characters`() {
        assertEquals("John", ValidationUtils.filterNameInput("John123"))
        assertEquals("MaryJane", ValidationUtils.filterNameInput("Mary@Jane"))
        assertEquals("Test", ValidationUtils.filterNameInput("Test!@#"))
    }
}