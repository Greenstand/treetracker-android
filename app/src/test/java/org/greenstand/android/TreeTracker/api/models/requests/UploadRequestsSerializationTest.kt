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

import android.os.Build
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the JSON produced for every request that ends up in an upload bundle. Field names are
 * part of the contract with the batch upload processor, so they are asserted explicitly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UploadRequestsSerializationTest {
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

    @Test
    fun `new tree request encodes snake case fields and round trips`() {
        val request =
            NewTreeRequest(
                userId = 3,
                uuid = "tree-uuid",
                lat = -1.29,
                lon = 36.82,
                gpsAccuracy = 4,
                note = "note",
                timestamp = 1_682_935_200L,
                imageUrl = "https://images/tree.jpg",
                sequenceId = 9L,
                deviceIdentifier = "device",
                planterPhotoUrl = "https://images/planter.jpg",
                planterIdentifier = "planter@test",
                attributes = listOf(AttributeRequest(key = "dbh", value = "10")),
            )

        val encoded = json.encodeToString(request)

        assertEquals(
            setOf(
                "user_id",
                "uuid",
                "lat",
                "lon",
                "gps_accuracy",
                "note",
                "timestamp",
                "image_url",
                "sequence_id",
                "device_identifier",
                "planter_photo_url",
                "planter_identifier",
                "attributes",
            ),
            parse(encoded).keys,
        )
        assertEquals(request, json.decodeFromString<NewTreeRequest>(encoded))
    }

    @Test
    fun `new tree request defaults encode as zeros and nulls`() {
        val encoded = parse(json.encodeToString(NewTreeRequest()))

        assertEquals(0, encoded["user_id"]!!.jsonPrimitive.int)
        assertEquals(0, encoded["sequence_id"]!!.jsonPrimitive.int)
        assertEquals(JsonNull, encoded["uuid"])
        assertEquals(JsonNull, encoded["attributes"])
    }

    @Test
    fun `registration request uses the device id by default`() {
        val request =
            RegistrationRequest(
                planterIdentifier = "planter@test",
                firstName = "Jane",
                lastName = "Doe",
                organization = null,
                phone = null,
                email = "jane@test.com",
                lat = 1.0,
                lon = 2.0,
                recordUuid = "record-uuid",
                imageUrl = "https://images/planter.jpg",
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals("test-device-id", encoded["device_identifier"]!!.jsonPrimitive.content)
        assertEquals("planter@test", encoded["planter_identifier"]!!.jsonPrimitive.content)
        assertEquals("record-uuid", encoded["record_uuid"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, encoded["organization"])
        assertEquals(request, json.decodeFromString<RegistrationRequest>(json.encodeToString(request)))
    }

    @Test
    fun `wallet registration request encodes api field names`() {
        val request =
            WalletRegistrationRequest(
                registrationId = "user-uuid",
                wallet = "wallet",
                imageUrl = "https://images/user.jpg",
                firstName = "John",
                lastName = "Doe",
                phone = null,
                email = "john@test.com",
                lat = 1.0,
                lon = 2.0,
                createdAt = "2023-01-01T00:00:00Z",
            )

        val encoded = json.encodeToString(request)

        assertEquals(
            setOf("id", "wallet", "user_photo_url", "first_name", "last_name", "phone", "email", "lat", "lon", "registered_at"),
            parse(encoded).keys,
        )
        assertEquals(request, json.decodeFromString<WalletRegistrationRequest>(encoded))
    }

    @Test
    fun `session request encodes api field names`() {
        val request =
            SessionRequest(
                sessionId = "session-uuid",
                originUserId = "user-uuid",
                targetWallet = "target",
                organization = "org",
                deviceConfigId = "config-uuid",
            )

        val encoded = json.encodeToString(request)

        assertEquals(
            setOf("id", "originating_wallet_registration_id", "target_wallet", "organization", "device_configuration_id"),
            parse(encoded).keys,
        )
        assertEquals(request, json.decodeFromString<SessionRequest>(encoded))
    }

    @Test
    fun `tracks request encodes sessions with their locations`() {
        val request =
            TracksRequest(
                sessionId = "session-uuid",
                locations =
                    listOf(
                        LocationRequest(accuracy = 3.5f, capturedAt = "2023-01-01T00:00:01Z", latitude = 1.0, longitude = 2.0),
                    ),
            )

        val encoded = json.encodeToString(request)

        val location = parse(encoded)["locations"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("accuracy", "capturedAt", "latitude", "longitude"), location.keys)
        assertEquals(request, json.decodeFromString<TracksRequest>(encoded))
    }

    @Test
    fun `tree capture request encodes api field names including nulls`() {
        val request =
            TreeCaptureRequest(
                sessionId = "session-uuid",
                treeId = "tree-uuid",
                lat = 1.0,
                lon = 2.0,
                note = null,
                imageUrl = "https://images/tree.jpg",
                createdAt = "2023-01-01T00:00:00Z",
                stepCount = 12L,
                deltaStepCount = null,
                rotationMatrix = null,
                extraAttributes = null,
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals(
            setOf(
                "session_id",
                "id",
                "lat",
                "lon",
                "note",
                "image_url",
                "captured_at",
                "abs_step_count",
                "delta_step_count",
                "rotation_matrix",
                "extra_attributes",
            ),
            encoded.keys,
        )
        assertEquals(12, encoded["abs_step_count"]!!.jsonPrimitive.int)
        assertEquals(JsonNull, encoded["note"])
    }

    @Test
    fun `device config request fills device details from the build`() {
        val request =
            DeviceConfigRequest(
                id = "config-uuid",
                appVersion = "1.0",
                appBuild = 7,
                osVersion = "14",
                sdkVersion = 34,
                instanceId = "instance",
                loggedAt = "2023-01-01T00:00:00Z",
            )

        val encoded = parse(json.encodeToString(request))

        assertEquals("test-device-id", encoded["device_identifier"]!!.jsonPrimitive.content)
        assertEquals(Build.MANUFACTURER, encoded["manufacturer"]!!.jsonPrimitive.content)
        assertEquals(Build.BRAND, encoded["brand"]!!.jsonPrimitive.content)
        assertEquals(Build.MODEL, encoded["model"]!!.jsonPrimitive.content)
        assertEquals(Build.HARDWARE, encoded["hardware"]!!.jsonPrimitive.content)
        assertEquals(Build.DEVICE, encoded["device"]!!.jsonPrimitive.content)
        assertEquals("unknown", encoded["serial"]!!.jsonPrimitive.content)
        assertEquals(7, encoded["app_build"]!!.jsonPrimitive.int)
        assertEquals(34, encoded["sdk_version"]!!.jsonPrimitive.int)
        assertEquals("instance", encoded["instance_id"]!!.jsonPrimitive.content)
        assertEquals("2023-01-01T00:00:00Z", encoded["logged_at"]!!.jsonPrimitive.content)
    }

    @Test
    fun `device request fills app and os details from the build`() {
        val request = DeviceRequest(instanceId = "instance")

        assertEquals("test-device-id", request.device_identifier)
        assertEquals(BuildConfig.VERSION_NAME, request.app_version)
        assertEquals(BuildConfig.VERSION_CODE, request.app_build)
        assertEquals(Build.VERSION.RELEASE, request.androidRelease)
        assertEquals(Build.VERSION.SDK_INT, request.androidSdkVersion)
        assertEquals("unknown", request.serial)
        assertEquals(request, json.decodeFromString<DeviceRequest>(json.encodeToString(request)))
    }

    @Test
    fun `v1 bundle carries trees registrations and the device`() {
        val tree = NewTreeRequest(uuid = "tree-uuid")
        val bundle =
            UploadBundle.createV1(
                newTreeRequests = listOf(tree),
                registrations = null,
                instanceId = "instance",
            )

        assertEquals("1", bundle.version)
        assertEquals("test-device-id", bundle.deviceId)
        assertEquals(listOf(tree), bundle.trees)
        assertEquals(listOf(DeviceRequest(instanceId = "instance")), bundle.devices)
        assertEquals(null, bundle.registrations)
        assertEquals(null, bundle.treeCaptures)
        assertEquals(null, bundle.walletRegistrations)
    }

    @Test
    fun `v2 bundle carries only the given v2 sections`() {
        val session = SessionRequest("session-uuid", "user-uuid", "target", "org", "config-uuid")
        val bundle = UploadBundle.createV2(sessions = listOf(session))

        assertEquals("2", bundle.version)
        assertEquals(listOf(session), bundle.sessions)
        assertEquals(null, bundle.devices)
        assertEquals(null, bundle.trees)
        assertEquals(null, bundle.tracks)
        assertEquals(null, bundle.messages)
        assertEquals(null, bundle.deviceConfig)
    }

    @Test
    fun `bundle encodes every section key with nulls for absent sections`() {
        val message =
            MessageRequest(
                id = "msg-1",
                parentMessageId = null,
                recipientHandle = "admin",
                authorHandle = "planter",
                type = "message",
                body = "Hi",
                surveyResponse = null,
                surveyId = null,
                composedAt = "2023-01-01T00:00:00Z",
            )

        val encoded = parse(json.encodeToString(UploadBundle.createV2(messages = listOf(message))))

        assertEquals(
            setOf(
                "pack_format_version",
                "device_id",
                "trees",
                "registrations",
                "devices",
                "wallet_registrations",
                "captures",
                "device_configurations",
                "sessions",
                "tracks",
                "messages",
            ),
            encoded.keys,
        )
        assertEquals("2", encoded["pack_format_version"]!!.jsonPrimitive.content)
        assertEquals("test-device-id", encoded["device_id"]!!.jsonPrimitive.content)
        assertEquals(1, encoded["messages"]!!.jsonArray.size)
        assertEquals(JsonNull, encoded["captures"])
        assertTrue(encoded["trees"] is JsonNull)
    }

    private fun parse(encoded: String): JsonObject = json.parseToJsonElement(encoded).jsonObject
}