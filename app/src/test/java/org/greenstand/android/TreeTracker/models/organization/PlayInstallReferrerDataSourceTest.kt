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
package org.greenstand.android.TreeTracker.models.organization

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Drives [PlayInstallReferrerDataSource] through a mocked [InstallReferrerClient], so no
 * Play Store connection is needed. The listener is invoked synchronously from
 * `startConnection`, as the real client does when it fails fast.
 */
class PlayInstallReferrerDataSourceTest {
    private val context: Context = mockk(relaxed = true)
    private val client: InstallReferrerClient = mockk(relaxed = true)
    private val listener = slot<InstallReferrerStateListener>()

    @Before
    fun setUp() {
        val builder: InstallReferrerClient.Builder = mockk()
        every { builder.build() } returns client
        mockkStatic(InstallReferrerClient::class)
        every { InstallReferrerClient.newBuilder(any()) } returns builder
    }

    @After
    fun tearDown() {
        unmockkStatic(InstallReferrerClient::class)
    }

    private fun givenConnection(onConnect: (InstallReferrerStateListener) -> Unit) {
        every { client.startConnection(capture(listener)) } answers { onConnect(listener.captured) }
    }

    private fun givenSetupFinishes(responseCode: Int) {
        givenConnection { it.onInstallReferrerSetupFinished(responseCode) }
    }

    private fun givenReferrer(referrer: String?) {
        val details: ReferrerDetails = mockk()
        every { details.installReferrer } returns referrer
        every { client.installReferrer } returns details
    }

    private suspend fun fetch(): InstallReferrerResult = PlayInstallReferrerDataSource(context).fetch()

    @Test
    fun `ok response returns the install referrer`() =
        runTest {
            givenReferrer("org_id=42&org_name=Acme")
            givenSetupFinishes(InstallReferrerResponse.OK)

            assertEquals(InstallReferrerResult.Available("org_id=42&org_name=Acme"), fetch())
        }

    @Test
    fun `ok response with a null referrer is available without a referrer`() =
        runTest {
            givenReferrer(null)
            givenSetupFinishes(InstallReferrerResponse.OK)

            assertEquals(InstallReferrerResult.Available(null), fetch())
        }

    @Test
    fun `ok response whose referrer cannot be read is available without a referrer`() =
        runTest {
            every { client.installReferrer } throws IllegalStateException("service died")
            givenSetupFinishes(InstallReferrerResponse.OK)

            assertEquals(InstallReferrerResult.Available(null), fetch())
        }

    @Test
    fun `feature not supported is reported as unsupported`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

            assertEquals(InstallReferrerResult.Unsupported, fetch())
        }

    @Test
    fun `service unavailable is reported as unavailable`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.SERVICE_UNAVAILABLE)

            assertEquals(InstallReferrerResult.Unavailable, fetch())
        }

    @Test
    fun `developer error is reported as unavailable`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.DEVELOPER_ERROR)

            assertEquals(InstallReferrerResult.Unavailable, fetch())
        }

    @Test
    fun `service disconnected response is reported as unavailable`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.SERVICE_DISCONNECTED)

            assertEquals(InstallReferrerResult.Unavailable, fetch())
        }

    @Test
    fun `a disconnect before setup finishes is reported as unavailable`() =
        runTest {
            givenConnection { it.onInstallReferrerServiceDisconnected() }

            assertEquals(InstallReferrerResult.Unavailable, fetch())
        }

    @Test
    fun `only the first callback decides the result`() =
        runTest {
            givenReferrer("org_id=42")
            givenConnection {
                it.onInstallReferrerServiceDisconnected()
                it.onInstallReferrerSetupFinished(InstallReferrerResponse.OK)
            }

            assertEquals(InstallReferrerResult.Unavailable, fetch())
        }

    @Test
    fun `a disconnect after a successful setup keeps the referrer`() =
        runTest {
            givenReferrer("org_id=42")
            givenConnection {
                it.onInstallReferrerSetupFinished(InstallReferrerResponse.OK)
                it.onInstallReferrerServiceDisconnected()
            }

            assertEquals(InstallReferrerResult.Available("org_id=42"), fetch())
        }

    @Test
    fun `the client is built from the given context`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

            fetch()

            verify { InstallReferrerClient.newBuilder(context) }
        }

    @Test
    fun `the connection is ended after fetching`() =
        runTest {
            givenSetupFinishes(InstallReferrerResponse.SERVICE_UNAVAILABLE)

            fetch()

            verify(exactly = 1) { client.endConnection() }
        }

    @Test
    fun `a failing endConnection does not change the result`() =
        runTest {
            every { client.endConnection() } throws IllegalStateException("already closed")
            givenReferrer("org_id=42")
            givenSetupFinishes(InstallReferrerResponse.OK)

            assertEquals(InstallReferrerResult.Available("org_id=42"), fetch())
        }
}