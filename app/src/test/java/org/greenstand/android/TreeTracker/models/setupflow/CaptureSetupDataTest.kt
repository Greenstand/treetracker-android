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
package org.greenstand.android.TreeTracker.models.setupflow

import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.user.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureSetupDataTest {
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)
    private val data = CaptureSetupData(exceptionDataCollector)

    private val user =
        User(
            id = 7L,
            wallet = "planter-wallet",
            numberOfTrees = 3,
            firstName = "Ada",
            lastName = null,
            photoPath = "/photos/ada.jpg",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    @Test
    fun `all fields start empty without reporting anything`() {
        assertNull(data.user)
        assertNull(data.destinationWallet)
        assertNull(data.sessionNote)
        assertNull(data.organizationName)
        confirmVerified(exceptionDataCollector)
    }

    @Test
    fun `setting the user stores it and reports its wallet`() {
        data.user = user

        assertEquals(user, data.user)
        verify { exceptionDataCollector.set(ExceptionDataCollector.USER_WALLET, "planter-wallet") }
    }

    @Test
    fun `clearing the user reports a null wallet`() {
        data.user = user

        data.user = null

        assertNull(data.user)
        verify { exceptionDataCollector.set(ExceptionDataCollector.USER_WALLET, null) }
    }

    @Test
    fun `setting the destination wallet stores and reports it`() {
        data.destinationWallet = "destination-wallet"

        assertEquals("destination-wallet", data.destinationWallet)
        verify { exceptionDataCollector.set(ExceptionDataCollector.DESTINATION_WALLET, "destination-wallet") }
    }

    @Test
    fun `setting the session note stores and reports it`() {
        data.sessionNote = "north field"

        assertEquals("north field", data.sessionNote)
        verify { exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, "north field") }
    }

    @Test
    fun `setting the organization name stores and reports it`() {
        data.organizationName = "Green Org"

        assertEquals("Green Org", data.organizationName)
        verify { exceptionDataCollector.set(ExceptionDataCollector.ORG_NAME, "Green Org") }
    }

    @Test
    fun `each field reports only its own key`() {
        data.sessionNote = "note"

        verify(exactly = 1) { exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, "note") }
        confirmVerified(exceptionDataCollector)
    }
}