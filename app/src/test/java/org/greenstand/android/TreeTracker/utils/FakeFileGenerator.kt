package org.greenstand.android.TreeTracker.utils

import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.signup.Credential
import org.greenstand.android.TreeTracker.signup.SignUpState

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

val fakeSignUpState = SignUpState(
    firstName = "Caleb",
    lastName = "Mzazi",
    email = "langat.caleb95@gmail.com",
    phone = "0713212213",
    photoPath = "some string",
    isCredentialValid = true,
    isCredentialView = true,
    existingUser = fakeUsers.first(),
    canGoToNextScreen = false,
    autofocusTextEnabled = false,
    isInternetAvailable = false,
    showPrivacyDialog = false,
    showSelfieTutorial = true,
)