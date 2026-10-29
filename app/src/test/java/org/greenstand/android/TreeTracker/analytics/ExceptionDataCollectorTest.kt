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
package org.greenstand.android.TreeTracker.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ExceptionDataCollectorTest {
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val collector = ExceptionDataCollector(crashlytics)

    @Test
    fun `set stores a string value as a custom key`() {
        collector.set(ExceptionDataCollector.ORG_NAME, "Greenstand")

        verify { crashlytics.setCustomKey(ExceptionDataCollector.ORG_NAME, "Greenstand") }
        verify(exactly = 0) { crashlytics.setUserId(any()) }
    }

    @Test
    fun `set with a null value is ignored`() {
        collector.set(ExceptionDataCollector.SESSION_NOTE, null)

        verify(exactly = 0) { crashlytics.setCustomKey(any(), any<String>()) }
    }

    @Test
    fun `set user wallet also sets the crashlytics user id`() {
        collector.set(ExceptionDataCollector.USER_WALLET, "wallet-1")

        verify { crashlytics.setUserId("wallet-1") }
        verify { crashlytics.setCustomKey(ExceptionDataCollector.USER_WALLET, "wallet-1") }
    }

    @Test
    fun `set power user wallet also sets the crashlytics user id`() {
        collector.set(ExceptionDataCollector.POWER_USER_WALLET, "power-wallet")

        verify { crashlytics.setUserId("power-wallet") }
        verify { crashlytics.setCustomKey(ExceptionDataCollector.POWER_USER_WALLET, "power-wallet") }
    }

    @Test
    fun `set stores a boolean value as a custom key`() {
        collector.set(ExceptionDataCollector.IS_SYNCING, true)

        verify { crashlytics.setCustomKey(ExceptionDataCollector.IS_SYNCING, true) }
    }

    @Test
    fun `clear blanks the custom key`() {
        collector.clear(ExceptionDataCollector.DESTINATION_WALLET)

        verify { crashlytics.setCustomKey(ExceptionDataCollector.DESTINATION_WALLET, "") }
        verify(exactly = 0) { crashlytics.setUserId(any()) }
    }

    @Test
    fun `clear of a wallet user key also blanks the user id`() {
        collector.clear(ExceptionDataCollector.USER_WALLET)
        collector.clear(ExceptionDataCollector.POWER_USER_WALLET)

        verify(exactly = 2) { crashlytics.setUserId("") }
        verify { crashlytics.setCustomKey(ExceptionDataCollector.USER_WALLET, "") }
        verify { crashlytics.setCustomKey(ExceptionDataCollector.POWER_USER_WALLET, "") }
    }

    @Test
    fun `first screen sets the route only`() {
        collector.setScreen("dashboard")

        verify { crashlytics.setCustomKey(ROUTE, "dashboard") }
        verify(exactly = 0) { crashlytics.setCustomKey(LAST_ROUTE, any<String>()) }
    }

    @Test
    fun `next screen moves the previous route to last route`() {
        collector.setScreen("dashboard")

        collector.setScreen("capture")

        verify { crashlytics.setCustomKey(ROUTE, "capture") }
        verify { crashlytics.setCustomKey(LAST_ROUTE, "dashboard") }
    }

    @Test
    fun `setting the same screen again changes nothing`() {
        collector.setScreen("dashboard")

        collector.setScreen("dashboard")

        verify(exactly = 1) { crashlytics.setCustomKey(ROUTE, "dashboard") }
    }

    @Test
    fun `routes keep tracking across several screens`() {
        collector.setScreen("dashboard")
        collector.setScreen("capture")

        collector.setScreen("review")

        verify { crashlytics.setCustomKey(ROUTE, "review") }
        verify { crashlytics.setCustomKey(LAST_ROUTE, "capture") }
    }

    companion object {
        // Mirrors the private keys in ExceptionDataCollector
        private const val ROUTE = "route"
        private const val LAST_ROUTE = "last_route"
    }
}