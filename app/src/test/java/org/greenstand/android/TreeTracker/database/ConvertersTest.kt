/*
 * Copyright 2023 Treetracker
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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class ConvertersTest {

    @Test
    fun `WHEN jsonToMap and mapToJson called THEN round-trip correctly`() = runTest {
        val originalMap = mapOf("key1" to "value1", "key2" to "value2")

        val json = Converters.mapToJson(originalMap)
        assertNotNull(json)

        val resultMap = Converters.jsonToMap(json)
        assertNotNull(resultMap)
        assertEquals(originalMap, resultMap)
    }

    @Test
    fun `WHEN jsonToMap receives null THEN returns null`() = runTest {
        val result = Converters.jsonToMap(null)

        assertNull(result)
    }

    @Test
    fun `WHEN mapToJson receives null THEN returns null string representation`() = runTest {
        val result = Converters.mapToJson(null)

        assertEquals("null", result)
    }

    @Test
    fun `WHEN instantToString called THEN converts Instant to ISO string`() = runTest {
        val instant = Instant.parse("2023-06-15T10:30:00Z")

        val result = Converters.instantToString(instant)

        assertEquals("2023-06-15T10:30:00Z", result)
    }

    @Test
    fun `WHEN stringToInstance called THEN converts ISO string to Instant`() = runTest {
        val isoString = "2023-06-15T10:30:00Z"

        val result = Converters.stringToInstance(isoString)

        assertNotNull(result)
        assertEquals(Instant.parse("2023-06-15T10:30:00Z"), result)
    }

    @Test
    fun `WHEN instantToString receives null THEN returns null`() = runTest {
        val result = Converters.instantToString(null)

        assertNull(result)
    }

    @Test
    fun `WHEN stringToArray and arrayToString called THEN round-trip correctly`() = runTest {
        val originalList = listOf("item1", "item2", "item3")

        val json = Converters.arrayToString(originalList)
        assertNotNull(json)

        val resultList = Converters.stringToArray(json)
        assertNotNull(resultList)
        assertEquals(originalList, resultList)
    }

    @Test
    fun `WHEN stringToArray receives null THEN returns null`() = runTest {
        val result = Converters.stringToArray(null)

        assertNull(result)
    }
}
