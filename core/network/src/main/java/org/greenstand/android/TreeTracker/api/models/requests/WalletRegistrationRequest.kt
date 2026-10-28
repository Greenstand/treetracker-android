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
package org.greenstand.android.TreeTracker.api.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletRegistrationRequest(
    @SerialName("id")
    val registrationId: String,
    @SerialName("wallet")
    val wallet: String,
    @SerialName("user_photo_url")
    val imageUrl: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("lat")
    val lat: Double,
    @SerialName("lon")
    val lon: Double,
    @SerialName("registered_at")
    val createdAt: String,
)