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
package org.greenstand.android.TreeTracker.screenshot

import org.greenstand.android.TreeTracker.signup.Credential
import org.greenstand.android.TreeTracker.signup.CredentialEntryView
import org.greenstand.android.TreeTracker.signup.NameEntryView
import org.greenstand.android.TreeTracker.signup.SignUpState
import org.junit.Test

class SignUpScreenshotTest : ScreenshotTest() {
    @Test
    fun signup_credential_phone_default() =
        snapshot {
            CredentialEntryView(
                state =
                    SignUpState(
                        showPrivacyDialog = false,
                    ),
            )
        }

    @Test
    fun signup_credential_email() =
        snapshot {
            CredentialEntryView(
                state =
                    SignUpState(
                        credential = Credential.Email(),
                        showPrivacyDialog = false,
                    ),
            )
        }

    @Test
    fun signup_privacy_dialog() =
        snapshot {
            CredentialEntryView(
                state =
                    SignUpState(
                        showPrivacyDialog = true,
                    ),
            )
        }

    @Test
    fun signup_name_entry_default() =
        snapshot {
            NameEntryView(
                state =
                    SignUpState(
                        isCredentialView = false,
                    ),
            )
        }

    @Test
    fun signup_name_entry_with_names() =
        snapshot {
            NameEntryView(
                state =
                    SignUpState(
                        isCredentialView = false,
                        firstName = "John",
                        lastName = "Doe",
                    ),
            )
        }
}