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
package org.greenstand.android.TreeTracker.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * An account created by an agent.
 *
 * @property userId Local database id of the user.
 * @property wallet The phone number or email the account signs in with.
 * @property firstName The account's first name.
 * @property lastName The account's last name.
 * @property isPowerUser True for the device admin. The first account on a device is the admin,
 *   and its existence is what skips the first-run setup flow.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AgentAccount(
    val userId: Long,
    val wallet: String,
    val firstName: String,
    val lastName: String,
    val isPowerUser: Boolean,
)

/**
 * Trees saved by an agent.
 *
 * @property sessionId Local database id of the session the trees were saved in.
 * @property wallet Wallet of the account the trees were captured by.
 * @property treeCount Number of trees saved.
 * @property treeUuids UUIDs of the saved trees.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AgentCapturedTrees(
    val sessionId: Long,
    val wallet: String,
    val treeCount: Int,
    val treeUuids: List<String>,
)