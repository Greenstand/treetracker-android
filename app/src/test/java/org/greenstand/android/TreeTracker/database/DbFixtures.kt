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
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity

/** Entity factories with fixed, deterministic values for the DAO tests. */
object DbFixtures {
    val CREATED_AT: Instant = Instant.parse("2024-03-01T10:15:30Z")

    fun user(
        wallet: String = "wallet",
        uuid: String = "user-uuid",
        uploaded: Boolean = false,
        powerUser: Boolean = false,
        bundleId: String? = null,
        phone: String? = "+15550100",
        email: String? = null,
        photoUrl: String? = null,
    ) = UserEntity(
        uuid = uuid,
        wallet = wallet,
        firstName = "Ada",
        lastName = "Lovelace",
        phone = phone,
        email = email,
        latitude = 1.25,
        longitude = 2.5,
        uploaded = uploaded,
        createdAt = CREATED_AT,
        bundleId = bundleId,
        photoPath = "/selfie.jpg",
        photoUrl = photoUrl,
        powerUser = powerUser,
    )

    fun deviceConfig(
        uuid: String = "device-config-uuid",
        loggedAt: Instant = CREATED_AT,
        isUploaded: Boolean = false,
        bundleId: String? = null,
    ) = DeviceConfigEntity(
        uuid = uuid,
        appVersion = "2.2.0",
        appBuild = 220,
        osVersion = "14",
        sdkVersion = 34,
        loggedAt = loggedAt,
        isUploaded = isUploaded,
        bundleId = bundleId,
    )

    fun session(
        uuid: String = "session-uuid",
        originWallet: String = "wallet",
        isUploaded: Boolean = false,
        deviceConfigId: Long? = null,
        endTime: Instant? = null,
        organization: String? = "org",
        note: String? = null,
    ) = SessionEntity(
        uuid = uuid,
        originUserId = "user-uuid",
        originWallet = originWallet,
        destinationWallet = "destination-wallet",
        startTime = CREATED_AT,
        endTime = endTime,
        organization = organization,
        isUploaded = isUploaded,
        deviceConfigId = deviceConfigId,
        note = note,
    )

    fun tree(
        sessionId: Long,
        uuid: String = "tree-uuid",
        photoPath: String? = "/tree.jpg",
        photoUrl: String? = null,
        uploaded: Boolean = false,
        bundleId: String? = null,
        extraAttributes: Map<String, String>? = null,
    ) = TreeEntity(
        uuid = uuid,
        sessionId = sessionId,
        photoPath = photoPath,
        photoUrl = photoUrl,
        note = "a note",
        latitude = 3.5,
        longitude = 4.75,
        uploaded = uploaded,
        createdAt = CREATED_AT,
        bundleId = bundleId,
        extraAttributes = extraAttributes,
    )

    fun location(
        sessionId: Long,
        json: String = "{\"lat\":1.0}",
    ) = LocationEntity(
        locationDataJson = json,
        sessionId = sessionId,
    )

    fun planterInfo(
        identifier: String = "planter@example.com",
        uploaded: Boolean = false,
        bundleId: String? = null,
        organization: String? = "org",
    ) = PlanterInfoEntity(
        identifier = identifier,
        firstName = "Grace",
        lastName = "Hopper",
        organization = organization,
        phone = null,
        email = identifier,
        latitude = 5.5,
        longitude = 6.5,
        uploaded = uploaded,
        createdAt = 1_600_000_000L,
        bundleId = bundleId,
        recordUuid = "record-$identifier",
    )

    fun planterCheckIn(
        planterInfoId: Long,
        localPhotoPath: String? = "/check-in.jpg",
        photoUrl: String? = null,
    ) = PlanterCheckInEntity(
        planterInfoId = planterInfoId,
        localPhotoPath = localPhotoPath,
        photoUrl = photoUrl,
        latitude = 7.5,
        longitude = 8.5,
        createdAt = 1_600_000_100L,
    )

    fun treeCapture(
        planterCheckInId: Long,
        uuid: String = "capture-uuid",
        localPhotoPath: String? = "/capture.jpg",
        photoUrl: String? = null,
        uploaded: Boolean = false,
        latitude: Double = 9.5,
        longitude: Double = 10.5,
    ) = TreeCaptureEntity(
        uuid = uuid,
        planterCheckInId = planterCheckInId,
        localPhotoPath = localPhotoPath,
        photoUrl = photoUrl,
        noteContent = "capture note",
        latitude = latitude,
        longitude = longitude,
        accuracy = 4.0,
        uploaded = uploaded,
        createAt = 1_600_000_200L,
    )

    fun treeAttribute(
        treeCaptureId: Long,
        key: String = "height",
        value: String = "3",
    ) = TreeAttributeEntity(
        key = key,
        value = value,
        treeCaptureId = treeCaptureId,
    )

    fun org(
        id: String = "org-id",
        name: String = "Greenstand",
        version: Int = 1,
    ) = OrganizationEntity(
        id = id,
        version = version,
        name = name,
        walletId = "org-wallet",
        captureSetupFlowJson = "{\"setup\":true}",
        captureFlowJson = "{\"capture\":true}",
    )
}