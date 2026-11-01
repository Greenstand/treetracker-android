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
package org.greenstand.android.TreeTracker.database

import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Edge inputs for the Room [Converters] shared by AppDatabase and MessageDatabase. ConvertersTest covers the basics. */
class ConvertersEdgeCasesTest {
    @Test
    fun `empty map round trips as an empty JSON object`() {
        val json = Converters.mapToJson(emptyMap())

        assertEquals("{}", json)
        assertEquals(emptyMap(), Converters.jsonToMap(json))
    }

    @Test
    fun `map values with quotes, commas, newlines and unicode round trip unchanged`() {
        val original =
            mapOf(
                "note" to "said \"hi\", then left",
                "multi" to "line one\nline two",
                "species" to "Acacia été 🌳",
                "" to "empty key",
            )

        assertEquals(original, Converters.jsonToMap(Converters.mapToJson(original)))
    }

    @Test
    fun `mapToJson keeps insertion order of the keys`() {
        val json = Converters.mapToJson(linkedMapOf("b" to "2", "a" to "1"))

        assertEquals("""{"b":"2","a":"1"}""", json)
    }

    @Test
    fun `jsonToMap ignores whitespace in stored JSON`() {
        assertEquals(mapOf("height" to "3"), Converters.jsonToMap("  { \"height\" : \"3\" }  "))
    }

    @Test
    fun `jsonToMap returns null when a value is not a string`() {
        assertNull(Converters.jsonToMap("""{"height":3}"""))
    }

    @Test
    fun `jsonToMap returns null for a JSON array or the JSON null literal`() {
        assertNull(Converters.jsonToMap("""["a","b"]"""))
        assertNull(Converters.jsonToMap("null"))
    }

    @Test
    fun `jsonToMap returns null for an empty string`() {
        assertNull(Converters.jsonToMap(""))
    }

    @Test
    fun `empty list round trips as an empty JSON array`() {
        val json = Converters.arrayToString(emptyList())

        assertEquals("[]", json)
        assertEquals(emptyList(), Converters.stringToArray(json))
    }

    @Test
    fun `list keeps order, duplicates and special characters`() {
        val original = listOf("Yes", "No", "Yes", "1, 2 or \"3\"", "")

        assertEquals(original, Converters.stringToArray(Converters.arrayToString(original)))
    }

    @Test
    fun `stringToArray returns null for an array of numbers or an object`() {
        assertNull(Converters.stringToArray("[1,2]"))
        assertNull(Converters.stringToArray("""{"a":"b"}"""))
    }

    @Test
    fun `instant with nanoseconds round trips exactly`() {
        val instant = Instant.fromEpochSeconds(1_709_288_130L, 123_456_789)

        val stored = Converters.instantToString(instant)

        assertEquals("2024-03-01T10:15:30.123456789Z", stored)
        assertEquals(instant, Converters.stringToInstance(stored))
    }

    @Test
    fun `instant with milliseconds is stored with three fractional digits`() {
        val instant = Instant.fromEpochMilliseconds(1_709_288_130_500L)

        assertEquals("2024-03-01T10:15:30.500Z", Converters.instantToString(instant))
    }

    @Test
    fun `stringToInstance accepts a UTC offset and normalises it`() {
        assertEquals(Instant.parse("2024-03-01T10:15:30Z"), Converters.stringToInstance("2024-03-01T12:15:30+02:00"))
    }

    @Test
    fun `stringToInstance returns null for an empty string or a date without time`() {
        assertNull(Converters.stringToInstance(""))
        assertNull(Converters.stringToInstance("2024-03-01"))
    }

    @Test
    fun `instants before the epoch round trip`() {
        val instant = Instant.parse("1969-12-31T23:59:59Z")

        assertEquals(instant, Converters.stringToInstance(Converters.instantToString(instant)))
    }
}