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
package org.greenstand.android.TreeTracker.models.organization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OrgDataTest {
    @Test
    fun `destination features default to null`() {
        assertNull(Destination("user-select").features)
    }

    @Test
    fun `destination without features decodes with null features`() {
        val destination = orgTestJson.decodeFromString<Destination>("""{"route":"user-select"}""")

        assertEquals(Destination("user-select", null), destination)
    }

    @Test
    fun `destination decodes its feature list`() {
        val destination =
            orgTestJson.decodeFromString<Destination>(
                """{"route":"tree-image-review/{photoPath}","features":["forceNote"]}""",
            )

        assertEquals(Destination("tree-image-review/{photoPath}", listOf("forceNote")), destination)
    }

    @Test
    fun `destination encodes null features explicitly with the app json config`() {
        assertEquals(
            """{"route":"user-select","features":null}""",
            orgTestJson.encodeToString(Destination("user-select")),
        )
    }

    @Test
    fun `destination list survives an encode and decode round trip`() {
        val flow =
            listOf(
                Destination("user-select"),
                Destination("session-note", emptyList()),
                Destination("tree-image-review/{photoPath}", listOf("forceNote", "unknown")),
            )

        val decoded = orgTestJson.decodeFromString<List<Destination>>(orgTestJson.encodeToString(flow))

        assertEquals(flow, decoded)
    }

    @Test
    fun `destination without a route cannot be decoded`() {
        assertThrows(SerializationException::class.java) {
            orgTestJson.decodeFromString<Destination>("""{"features":["forceNote"]}""")
        }
    }

    @Test
    fun `orgs with the same fields are equal`() {
        val flow = listOf(Destination("user-select"))

        assertEquals(
            Org("1", "Acme", "w", "", flow, flow),
            Org("1", "Acme", "w", "", flow.toList(), flow.toList()),
        )
    }

    @Test
    fun `v1 json keys match the documented org link payload`() {
        val payload =
            """
            {"id":"109288091","version":"1","name":"Kasiki Hai","walletId":"klasdlk1-a0a23lmnzcln9o3",
             "captureSetupFlow":[{"route":"user-select"},{"route":"session-note"}],
             "captureFlow":[{"route":"capture/{profilePicUrl}"},{"route":"tree-height-selection"}]}
            """.trimIndent()

        val obj = Json.parseToJsonElement(payload).jsonObject

        assertEquals("109288091", obj.getValue(OrgJsonKeys.V1.ID).jsonPrimitive.content)
        assertEquals("1", obj.getValue(OrgJsonKeys.V1.VERSION).jsonPrimitive.content)
        assertEquals("Kasiki Hai", obj.getValue(OrgJsonKeys.V1.NAME).jsonPrimitive.content)
        assertEquals("klasdlk1-a0a23lmnzcln9o3", obj.getValue(OrgJsonKeys.V1.WALLET_ID).jsonPrimitive.content)
        assertEquals(2, obj.getValue(OrgJsonKeys.V1.CAPTURE_SETUP_FLOW).jsonArray.size)
        assertEquals(2, obj.getValue(OrgJsonKeys.V1.CAPTURE_FLOW).jsonArray.size)
    }
}