package org.greenstand.android.TreeTracker.utils

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `valid names should pass validation`() {
        val validNames = listOf(
            "John",
            "Mary-Jane",
            "O'Connor",
            "José María",
            "李明",
            "François"
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