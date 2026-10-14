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

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.service.AppFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * AppFunctions that let agents set up test data without driving the UI. Debug and dev builds
 * only. The library instantiates this class through its no-arg constructor on each call.
 *
 * See docs/testing/agent-app-functions.md for how to call them over adb.
 */
class AgentAppFunctions : KoinComponent {
    /**
     * Creates a planter account without the sign-up screens, using a test image as the selfie.
     * The first account on a device becomes its admin, which skips the first-run setup flow on
     * the next launch. Later calls add more accounts. Restart the app to see a new account.
     *
     * @param firstName First name, at least 2 letters. Defaults to "Test".
     * @param lastName Last name, at least 2 letters. Defaults to "User".
     * @param phone Phone number of 7 to 15 digits to sign in with. Leave out phone and email to
     *   get a random unused phone number.
     * @param email Email to sign in with, instead of phone.
     * @return The created account.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createAccount(
        appFunctionContext: AppFunctionContext,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        email: String? = null,
    ): AgentAccount =
        runAgentFunction {
            AgentAccountCreator(get(), get()).createAccount(firstName, lastName, phone, email)
        }

    /**
     * Saves trees as if the planter captured them, using a test image and skipping the camera
     * and GPS wait. With no capture session running, the trees go in a new session that is
     * ended straight away. If the app is in the capture flow, they are added to that session.
     *
     * @param count Number of trees to save, 1 to 500.
     * @param wallet Wallet (phone or email) of the account capturing the trees. Defaults to the
     *   device admin.
     * @param latitude Latitude to place the trees around. Pass with longitude. Defaults to the
     *   device's location while the app is open, otherwise 0.
     * @param longitude Longitude to place the trees around. Pass with latitude.
     * @param note Note saved on every tree.
     * @return The saved trees.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun captureTrees(
        appFunctionContext: AppFunctionContext,
        count: Int,
        wallet: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        note: String? = null,
    ): AgentCapturedTrees =
        runAgentFunction {
            AgentTreeCapturer(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
                .captureTrees(count, wallet, latitude, longitude, note)
        }

    // AppFunctions are called on the main thread
    private suspend fun <T> runAgentFunction(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: IllegalArgumentException) {
                throw AppFunctionInvalidArgumentException(e.message)
            }
        }
}