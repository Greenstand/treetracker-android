package org.greenstand.android.TreeTracker.utils

import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.user.User

val fakeUsers = listOf(
    User(
        id = 122,
        wallet = "some random text",
        numberOfTrees = 4,
        firstName = "Caleb",
        lastName = "Kaleb",
        isPowerUser = true,
        photoPath = "some random text",
        unreadMessagesAvailable = false
    ),
    User(
        id = 155,
        wallet = "some random text",
        numberOfTrees = 7,
        firstName = "Jane",
        lastName = "Joseph",
        isPowerUser = false,
        photoPath = "some random text",
        unreadMessagesAvailable = true
    ),
    User(
        id = 48848,
        wallet = "some random text",
        numberOfTrees = 11,
        firstName = "Mike",
        lastName = "Vincent",
        isPowerUser = true,
        photoPath = "some random text",
        unreadMessagesAvailable = false
    )
)

val emptyUser = User(
    id = 122,
    wallet = "some random text",
    numberOfTrees = 4,
    firstName = "",
    lastName = "",
    isPowerUser = true,
    photoPath = "some random text",
    unreadMessagesAvailable = false
)

val fakePlanterInfo = PlanterInfoEntity(
    identifier = "random",
    firstName = "Caleb",
    lastName = "Langat",
    organization = "Greenstand",
    phone = "+2548171311",
    email = null,
    latitude = 12.11,
    longitude = 15.13,
    uploaded = false,
    createdAt = 13131,
    bundleId = null,
    recordUuid = "random"
)

val fakeUser = UserEntity(
    uuid = "random",
    wallet = "string",
    firstName = "Jay",
    lastName = "Ray",
    phone = "07151515120",
    email = null,
    latitude = 12.222,
    longitude = 1212.1212,
    uploaded = false,
    createdAt = Instant.DISTANT_FUTURE,
    bundleId = null,
    photoPath = "lol",
    photoUrl = "anotherString",
    powerUser = true
)

val fakePlanterCheckInEntity = PlanterCheckInEntity(
    planterInfoId = 1,
    localPhotoPath = "new",
    photoUrl = "String",
    latitude = 17.111,
    longitude = 12.131,
    createdAt = 121212
)
val fakeTree = listOf(
    TreeEntity(
        uuid = "just some string",
        sessionId = 1921,
        photoUrl = "random photo",
        photoPath = "random photo path",
        note = "string",
        latitude = 88.11,
        longitude = 99.11,
        uploaded = false,
        createdAt = Instant.DISTANT_FUTURE,
        bundleId = "bundled",
        extraAttributes = null
    ),
    TreeEntity(
        uuid = "just a string",
        sessionId = 111,
        photoUrl = "random ",
        photoPath = "random path",
        note = "string",
        latitude = 88.11,
        longitude = 99.11,
        uploaded = false,
        createdAt = Instant.DISTANT_FUTURE,
        bundleId = "bundled",
        extraAttributes = null
    )
)
val fakeOrg = listOf(
    OrganizationEntity(
        id = "new",
        version = 4,
        name = "GreenStand",
        walletId = "wallet",
        captureFlowJson = "random",
        captureSetupFlowJson = "another random"
    ),
    OrganizationEntity(
        id = "random",
        version = 6,
        name = "Green",
        walletId = "wallet Id",
        captureFlowJson = "random one",
        captureSetupFlowJson = "first another random"
    )
)