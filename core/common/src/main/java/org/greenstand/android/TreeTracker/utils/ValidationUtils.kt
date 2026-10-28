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

/**
 * Utility class for validating user input fields
 */
object ValidationUtils {
    private val NAME_PATTERN = """^[\p{L}\s'-]+$""".toRegex()

    private val INVALID_CHARS_PATTERN = """[0-9!@#$%^&*()_+=\[\]{};:"|<>?,./\\~`]""".toRegex()

    /**
     * Validates a name field (first name or last name)
     * @param name The name to validate
     * @return Pair of (isValid, errorMessage)
     */
    fun validateName(name: String?): Pair<Boolean, String?> =
        when {
            name.isNullOrBlank() -> {
                false to "Name cannot be empty"
            }
            name.trim().length < 2 -> {
                false to "Name must be at least 2 characters"
            }
            name.length > 50 -> {
                false to "Name cannot exceed 50 characters"
            }
            INVALID_CHARS_PATTERN.containsMatchIn(name) -> {
                false to "Name cannot contain numbers or special characters"
            }
            !NAME_PATTERN.matches(name.trim()) -> {
                false to "Name can only contain letters, spaces, hyphens and apostrophes"
            }
            else -> {
                true to null
            }
        }

    /**
     * Filters out invalid characters from input
     */
    fun filterNameInput(input: String): String {
        return input
            .filter { char ->
                char.isLetter() || char.isWhitespace() || char == '-' || char == '\''
            }.take(50) // Also enforce max length
    }
}