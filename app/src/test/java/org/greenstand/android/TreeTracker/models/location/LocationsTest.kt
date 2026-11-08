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
package org.greenstand.android.TreeTracker.models.location

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.models.Accuracy
import org.greenstand.android.TreeTracker.models.ConvergenceStats
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationsTest {
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val locationData =
        LocationData(
            planterCheckInId = 3L,
            latitude = 37.42,
            longitude = -122.08,
            accuracy = 4.5f,
            treeUuid = "tree-uuid",
            convergenceStatus = ConvergenceStatus.CONVERGED,
            capturedAt = "2024-01-01T00:00:00Z",
        )

    @Test
    fun `location data survives a json round trip`() {
        val encoded = json.encodeToString(LocationData.serializer(), locationData)

        assertEquals(locationData, json.decodeFromString(LocationData.serializer(), encoded))
    }

    @Test
    fun `location data is encoded with the field names the server expects`() {
        val encoded = json.encodeToString(LocationData.serializer(), locationData)

        val fields = json.parseToJsonElement(encoded).jsonObject
        assertEquals(
            setOf(
                "planterCheckInId",
                "latitude",
                "longitude",
                "accuracy",
                "treeUuid",
                "convergenceStatus",
                "capturedAt",
            ),
            fields.keys,
        )
        assertEquals("CONVERGED", fields.getValue("convergenceStatus").jsonPrimitive.content)
    }

    @Test
    fun `location data outside tree capture keeps null tree uuid and status`() {
        val outsideCapture = locationData.copy(treeUuid = null, convergenceStatus = null)

        val encoded = json.encodeToString(LocationData.serializer(), outsideCapture)

        assertEquals(outsideCapture, json.decodeFromString(LocationData.serializer(), encoded))
        assertEquals(
            "null",
            json
                .parseToJsonElement(encoded)
                .jsonObject
                .getValue("treeUuid")
                .toString(),
        )
    }

    @Test
    fun `every convergence status survives a json round trip`() {
        ConvergenceStatus.entries.forEach { status ->
            val encoded = json.encodeToString(ConvergenceStatus.serializer(), status)

            assertEquals(status, json.decodeFromString(ConvergenceStatus.serializer(), encoded))
        }
    }

    @Test
    fun `convergence stats compare by value`() {
        assertEquals(ConvergenceStats(1.0, 2.0, 3.0), ConvergenceStats(1.0, 2.0, 3.0))
    }

    @Test
    fun `accuracy levels are good, bad and none`() {
        assertEquals(listOf(Accuracy.GOOD, Accuracy.BAD, Accuracy.NONE), Accuracy.entries.toList())
    }
}