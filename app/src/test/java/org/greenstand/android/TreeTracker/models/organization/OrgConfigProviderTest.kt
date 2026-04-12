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
package org.greenstand.android.TreeTracker.models.organization

import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class OrgConfigProviderTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var orgConfigProvider: OrgConfigProvider

    @Before
    fun setUp() {
        remoteConfig = mockk(relaxed = true)
        orgConfigProvider = OrgConfigProvider(remoteConfig)
    }

    @Test
    fun `WHEN Remote Config has value for org THEN returns config string`() =
        runTest {
            val configJson = """{"version":"1","walletId":"wallet-abc"}"""
            every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(true)
            every { remoteConfig.getString("org_123") } returns configJson

            val result = orgConfigProvider.fetchOrgConfig("123")

            assertEquals(configJson, result)
            verify { remoteConfig.getString("org_123") }
        }

    @Test
    fun `WHEN Remote Config returns blank string THEN returns null`() =
        runTest {
            every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(true)
            every { remoteConfig.getString("org_456") } returns ""

            val result = orgConfigProvider.fetchOrgConfig("456")

            assertNull(result)
        }

    @Test
    fun `WHEN fetchAndActivate throws THEN returns null`() =
        runTest {
            every { remoteConfig.fetchAndActivate() } returns Tasks.forException(RuntimeException("Network error"))

            val result = orgConfigProvider.fetchOrgConfig("789")

            assertNull(result)
        }

    @Test
    fun `WHEN Remote Config key not set THEN returns null`() =
        runTest {
            every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(false)
            every { remoteConfig.getString("org_unknown") } returns ""

            val result = orgConfigProvider.fetchOrgConfig("unknown")

            assertNull(result)
        }

    @Test
    fun `WHEN fetchAndActivate returns false but value exists THEN returns config`() =
        runTest {
            val configJson = """{"version":"1","walletId":"cached-wallet"}"""
            every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(false)
            every { remoteConfig.getString("org_cached") } returns configJson

            val result = orgConfigProvider.fetchOrgConfig("cached")

            assertEquals(configJson, result)
        }

    @Test
    fun `WHEN org ID has special characters THEN key is constructed correctly`() =
        runTest {
            every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(true)
            every { remoteConfig.getString("org_abc-123") } returns """{"version":"1"}"""

            val result = orgConfigProvider.fetchOrgConfig("abc-123")

            assertEquals("""{"version":"1"}""", result)
            verify { remoteConfig.getString("org_abc-123") }
        }
}