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
package org.greenstand.android.TreeTracker.api.models.requests

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exact JSON key sets and value encodings of the upload requests that UploadRequestsSerializationTest
 * doesn't pin down. The batch upload processor matches on these names, including the camelCase ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UploadRequestsJsonShapeTest {
    // Same configuration as the app's Json instance (see AppModule)
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Before
    fun setUp() {
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun parse(encoded: String): JsonObject = json.parseToJsonElement(encoded).jsonObject

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.objects(key: String): List<JsonObject> = getValue(key).jsonArray.map { it.jsonObject }

    private fun JsonObject.single(key: String): JsonObject = objects(key).single()

    private fun deviceConfigRequest() =
        DeviceConfigRequest(
            id = "config-uuid",
            appVersion = "2.2.0",
            appBuild = 220,
            osVersion = "14",
            sdkVersion = 34,
            instanceId = "instance",
            loggedAt = "2024-03-01T10:15:30Z",
        )

    @Test
    fun `device config request encodes exactly the api field names`() {
        val encoded = parse(json.encodeToString(deviceConfigRequest()))

        assertEquals(
            setOf(
                "id",
                "device_identifier",
                "app_version",
                "app_build",
                "manufacturer",
                "brand",
                "model",
                "hardware",
                "device",
                "serial",
                "os_version",
                "sdk_version",
                "instance_id",
                "logged_at",
            ),
            encoded.keys,
        )
        assertEquals("config-uuid", encoded["id"]!!.jsonPrimitive.content)
        assertEquals("2.2.0", encoded["app_version"]!!.jsonPrimitive.content)
        assertEquals("14", encoded["os_version"]!!.jsonPrimitive.content)
        assertTrue(encoded["os_version"]!!.jsonPrimitive.isString, "os_version is sent as a string")
        assertTrue(!encoded["app_build"]!!.jsonPrimitive.isString, "app_build is sent as a number")
    }

    @Test
    fun `device config request lets explicit device details override the build defaults`() {
        val request =
            DeviceConfigRequest(
                id = "config-uuid",
                deviceIdentifier = "other-device",
                appVersion = "2.2.0",
                appBuild = 220,
                manufacturer = "Acme",
                serial = "SN1",
                osVersion = "14",
                sdkVersion = 34,
                instanceId = "instance",
                loggedAt = "2024-03-01T10:15:30Z",
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals("other-device", encoded["device_identifier"]!!.jsonPrimitive.content)
        assertEquals("Acme", encoded["manufacturer"]!!.jsonPrimitive.content)
        assertEquals("SN1", encoded["serial"]!!.jsonPrimitive.content)
    }

    @Test
    fun `v1 device request mixes snake case and camel case keys`() {
        val encoded = parse(json.encodeToString(DeviceRequest(instanceId = "instance")))

        assertEquals(
            setOf(
                "device_identifier",
                "app_version",
                "app_build",
                "manufacturer",
                "brand",
                "model",
                "hardware",
                "device",
                "serial",
                "androidRelease",
                "androidSdkVersion",
                "instanceId",
            ),
            encoded.keys,
        )
        assertEquals("instance", encoded["instanceId"]!!.jsonPrimitive.content)
        assertEquals("test-device-id", encoded["device_identifier"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tracks request uses camel case keys at both levels`() {
        val request =
            TracksRequest(
                sessionId = "session-uuid",
                locations =
                    listOf(
                        LocationRequest(accuracy = 3.5f, capturedAt = "2024-03-01T10:15:30Z", latitude = -1.29, longitude = 36.82),
                        LocationRequest(accuracy = 0f, capturedAt = "2024-03-01T10:15:31Z", latitude = 0.0, longitude = 0.0),
                    ),
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals(setOf("sessionId", "locations"), encoded.keys)
        val locations = encoded["locations"]!!.jsonArray.map { it.jsonObject }
        assertEquals(2, locations.size)
        assertEquals(3.5f, locations[0]["accuracy"]!!.jsonPrimitive.float)
        assertEquals(-1.29, locations[0]["latitude"]!!.jsonPrimitive.double)
        assertEquals("2024-03-01T10:15:31Z", locations[1]["capturedAt"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tracks request with no locations encodes an empty array`() {
        val encoded = parse(json.encodeToString(TracksRequest(sessionId = "session-uuid", locations = emptyList())))

        assertTrue(encoded["locations"]!!.jsonArray.isEmpty())
    }

    @Test
    fun `tree capture request sends extra attributes and rotation matrix as raw strings`() {
        val request =
            TreeCaptureRequest(
                sessionId = "session-uuid",
                treeId = "tree-uuid",
                lat = -1.29,
                lon = 36.82,
                note = "note",
                imageUrl = "https://images/tree.jpg",
                createdAt = "2024-03-01T10:15:30Z",
                stepCount = 1_234L,
                deltaStepCount = 56L,
                rotationMatrix = "[1.0,0.0,0.0]",
                extraAttributes = """{"height":"3"}""",
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals("""{"height":"3"}""", encoded["extra_attributes"]!!.jsonPrimitive.content)
        assertTrue(encoded["extra_attributes"]!!.jsonPrimitive.isString)
        assertEquals("[1.0,0.0,0.0]", encoded["rotation_matrix"]!!.jsonPrimitive.content)
        assertEquals(56L, encoded["delta_step_count"]!!.jsonPrimitive.long)
        assertEquals("tree-uuid", encoded["id"]!!.jsonPrimitive.content)
        assertEquals("2024-03-01T10:15:30Z", encoded["captured_at"]!!.jsonPrimitive.content)
        assertEquals(-1.29, encoded["lat"]!!.jsonPrimitive.double)
    }

    @Test
    fun `tree capture request decodes back from its own json`() {
        val request =
            TreeCaptureRequest(
                sessionId = "session-uuid",
                treeId = "tree-uuid",
                lat = 1.0,
                lon = 2.0,
                note = null,
                imageUrl = "https://images/tree.jpg",
                createdAt = "2024-03-01T10:15:30Z",
                stepCount = null,
                deltaStepCount = null,
                rotationMatrix = null,
                extraAttributes = null,
            )

        val decoded = json.decodeFromString<TreeCaptureRequest>(json.encodeToString(request))

        assertEquals("session-uuid", decoded.sessionId)
        assertEquals("tree-uuid", decoded.treeId)
        assertEquals(null, decoded.stepCount)
        assertEquals(null, decoded.extraAttributes)
    }

    @Test
    fun `new tree request attributes encode as key value objects`() {
        val request =
            NewTreeRequest(
                uuid = "tree-uuid",
                attributes = listOf(AttributeRequest("height", "3"), AttributeRequest("species", "acacia")),
            )

        val attributes = parse(json.encodeToString(request))["attributes"]!!.jsonArray.map { it.jsonObject }

        assertEquals(listOf(setOf("key", "value"), setOf("key", "value")), attributes.map { it.keys })
        assertEquals(listOf("height", "species"), attributes.map { it["key"]!!.jsonPrimitive.content })
        assertEquals(listOf("3", "acacia"), attributes.map { it["value"]!!.jsonPrimitive.content })
    }

    @Test
    fun `new tree request decodes from an empty object using its defaults`() {
        assertEquals(NewTreeRequest(), json.decodeFromString<NewTreeRequest>("{}"))
    }

    @Test
    fun `new tree request keeps a timestamp beyond the int range`() {
        val encoded = parse(json.encodeToString(NewTreeRequest(timestamp = 4_102_444_800_000L)))

        assertEquals(4_102_444_800_000L, encoded["timestamp"]!!.jsonPrimitive.long)
    }

    @Test
    fun `registration request encodes exactly the api field names`() {
        val request =
            RegistrationRequest(
                planterIdentifier = null,
                firstName = null,
                lastName = null,
                organization = null,
                phone = "+15550100",
                email = null,
                lat = null,
                lon = null,
                recordUuid = "record-uuid",
                imageUrl = "https://images/planter.jpg",
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals(
            setOf(
                "planter_identifier",
                "first_name",
                "last_name",
                "organization",
                "phone",
                "email",
                "lat",
                "lon",
                "device_identifier",
                "record_uuid",
                "image_url",
            ),
            encoded.keys,
        )
        assertEquals(JsonNull, encoded["lat"])
        assertEquals(JsonNull, encoded["planter_identifier"])
        assertEquals("https://images/planter.jpg", encoded["image_url"]!!.jsonPrimitive.content)
    }

    @Test
    fun `wallet registration request fails to decode without a nullable field under explicit nulls`() {
        val missingPhone =
            """
            {"id":"u","wallet":"w","user_photo_url":"https://u","first_name":"A","last_name":"B",
             "email":null,"lat":1.0,"lon":2.0,"registered_at":"2024-03-01T10:15:30Z"}
            """.trimIndent()

        assertFailsWith<SerializationException> { json.decodeFromString<WalletRegistrationRequest>(missingPhone) }
    }

    @Test
    fun `v1 bundle encodes trees, registrations and one device with v2 sections null`() {
        val registration =
            RegistrationRequest(
                planterIdentifier = "planter@test",
                firstName = "Jane",
                lastName = "Doe",
                organization = "org",
                phone = null,
                email = "jane@test",
                lat = 1.0,
                lon = 2.0,
                recordUuid = "record-uuid",
                imageUrl = "https://images/planter.jpg",
            )
        val bundle =
            UploadBundle.createV1(
                newTreeRequests = listOf(NewTreeRequest(uuid = "t1"), NewTreeRequest(uuid = "t2")),
                registrations = listOf(registration),
                instanceId = "instance",
            )

        val encoded = parse(json.encodeToString(bundle))

        assertEquals("1", encoded["pack_format_version"]!!.jsonPrimitive.content)
        assertTrue(encoded["pack_format_version"]!!.jsonPrimitive.isString, "The version is sent as a string")
        assertEquals(listOf("t1", "t2"), encoded.objects("trees").map { it.string("uuid") })
        assertEquals("record-uuid", encoded.single("registrations").string("record_uuid"))
        assertEquals("instance", encoded.single("devices").string("instanceId"))
        listOf("wallet_registrations", "captures", "device_configurations", "sessions", "tracks", "messages").forEach {
            assertEquals(JsonNull, encoded[it], "$it should be null in a v1 bundle")
        }
    }

    @Test
    fun `v2 bundle with every section encodes each list under its api key`() {
        val bundle =
            UploadBundle.createV2(
                walletRegistration =
                    listOf(
                        WalletRegistrationRequest("u", "w", "https://u", "A", "B", null, null, 1.0, 2.0, "2024-03-01T10:15:30Z"),
                    ),
                treeCaptures =
                    listOf(
                        TreeCaptureRequest("s", "t", 1.0, 2.0, null, "https://t", "2024-03-01T10:15:30Z", null, null, null, null),
                    ),
                sessions = listOf(SessionRequest("s", "u", "w", "org", "c")),
                tracks = listOf(TracksRequest("s", emptyList())),
                deviceConfigs = listOf(deviceConfigRequest()),
            )

        val encoded = parse(json.encodeToString(bundle))

        assertEquals("2", encoded["pack_format_version"]!!.jsonPrimitive.content)
        assertEquals("u", encoded.single("wallet_registrations").string("id"))
        assertEquals("t", encoded.single("captures").string("id"))
        assertEquals("s", encoded.single("sessions").string("id"))
        assertEquals("s", encoded.single("tracks").string("sessionId"))
        assertEquals("config-uuid", encoded.single("device_configurations").string("id"))
        assertEquals(JsonNull, encoded["messages"])
        assertEquals(JsonNull, encoded["trees"])
        assertEquals(JsonNull, encoded["devices"])
    }

    @Test
    fun `v2 bundle with empty lists sends empty arrays rather than nulls`() {
        val encoded = parse(json.encodeToString(UploadBundle.createV2(sessions = emptyList(), treeCaptures = emptyList())))

        assertTrue(encoded["sessions"]!!.jsonArray.isEmpty())
        assertTrue(encoded["captures"]!!.jsonArray.isEmpty())
        assertEquals(JsonNull, encoded["tracks"])
    }

    @Test
    fun `bundle device id can be overridden`() {
        val encoded = parse(json.encodeToString(UploadBundle(version = "2", deviceId = "explicit")))

        assertEquals("explicit", encoded["device_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `session request sends an empty organization as an empty string`() {
        val encoded = parse(json.encodeToString(SessionRequest("s", "u", "w", "", "c")))

        assertEquals("", encoded["organization"]!!.jsonPrimitive.content)
        assertTrue(encoded["organization"]!!.jsonPrimitive.isString)
    }
}