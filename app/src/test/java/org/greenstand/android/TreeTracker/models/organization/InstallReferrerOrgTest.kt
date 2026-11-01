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

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstallReferrerOrgTest {
    private lateinit var prefs: Preferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        prefs = Preferences(context.getSharedPreferences("test-prefs", android.content.Context.MODE_PRIVATE))
    }

    private fun provider(
        vararg results: InstallReferrerResult,
        calls: MutableList<Int> = mutableListOf(),
    ) = InstallReferrerOrgProvider(
        prefs = prefs,
        dataSource = {
            val index = calls.size
            calls.add(index)
            results[index.coerceAtMost(results.lastIndex)]
        },
    )

    @Test
    fun `null referrer yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink(null))
    }

    @Test
    fun `blank referrer yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("   "))
    }

    @Test
    fun `organic install referrer yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("utm_source=google-play&utm_medium=organic"))
    }

    @Test
    fun `referrer without org id yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("org_name=Acme"))
    }

    @Test
    fun `referrer maps org id and name`() {
        assertEquals(OrgLink("42", "Acme"), InstallReferrerOrgProvider.parseOrgLink("org_id=42&org_name=Acme"))
    }

    @Test
    fun `referrer decodes an encoded org name`() {
        assertEquals(OrgLink("42", "Acme Trees"), InstallReferrerOrgProvider.parseOrgLink("org_id=42&org_name=Acme%20Trees"))
    }

    @Test
    fun `org name keeps ampersands and equals signs`() {
        assertEquals(OrgLink("9", "A&B=C Trees"), InstallReferrerOrgProvider.parseOrgLink("org_id=9&org_name=A%26B%3DC%20Trees"))
    }

    @Test
    fun `missing org name falls back to empty`() {
        assertEquals(OrgLink("42", ""), InstallReferrerOrgProvider.parseOrgLink("org_id=42"))
    }

    @Test
    fun `blank org id yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("org_id=&org_name=Acme"))
    }

    @Test
    fun `extra campaign params are ignored`() {
        assertEquals(OrgLink("42", "Acme"), InstallReferrerOrgProvider.parseOrgLink("utm_source=share&org_id=42&org_name=Acme&utm_medium=link"))
    }

    @Test
    fun `referrer is read only once`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Available("org_id=42&org_name=Acme"), calls = calls)

            assertEquals(OrgLink("42", "Acme"), provider.consume())
            assertNull(provider.consume())
            assertEquals(1, calls.size)
        }

    @Test
    fun `an organic install is consumed so later launches do not retry`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Available("utm_source=organic"), calls = calls)

            assertNull(provider.consume())
            assertNull(provider.consume())
            assertEquals(1, calls.size)
        }

    @Test
    fun `an unsupported play store is consumed`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Unsupported, calls = calls)

            assertNull(provider.consume())
            assertNull(provider.consume())
            assertEquals(1, calls.size)
        }

    @Test
    fun `a transient failure stays unconsumed and retries`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider =
                provider(
                    InstallReferrerResult.Unavailable,
                    InstallReferrerResult.Available("org_id=42&org_name=Acme"),
                    calls = calls,
                )

            assertNull(provider.consume())
            assertEquals(OrgLink("42", "Acme"), provider.consume())
            assertEquals(2, calls.size)
        }

    @Test
    fun `the consumed flag is shared across provider instances`() =
        runTest {
            val first = provider(InstallReferrerResult.Available("org_id=42&org_name=Acme"))
            assertEquals(OrgLink("42", "Acme"), first.consume())

            val second = provider(InstallReferrerResult.Available("org_id=42&org_name=Acme"))
            assertNull(second.consume())
        }

    @Test
    fun `plus signs in the org name decode as spaces`() {
        assertEquals(OrgLink("42", "Acme Trees"), InstallReferrerOrgProvider.parseOrgLink("org_id=42&org_name=Acme+Trees"))
    }

    @Test
    fun `empty org name yields an empty name`() {
        assertEquals(OrgLink("42", ""), InstallReferrerOrgProvider.parseOrgLink("org_id=42&org_name="))
    }

    @Test
    fun `an encoded blank org id yields no org`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("org_id=%20%20&org_name=Acme"))
    }

    @Test
    fun `the first org id wins when it is repeated`() {
        assertEquals(OrgLink("1", "Acme"), InstallReferrerOrgProvider.parseOrgLink("org_id=1&org_id=2&org_name=Acme"))
    }

    @Test
    fun `an encoded org id is decoded`() {
        assertEquals(OrgLink("a b", ""), InstallReferrerOrgProvider.parseOrgLink("org_id=a%20b"))
    }

    @Test
    fun `param names are case sensitive`() {
        assertNull(InstallReferrerOrgProvider.parseOrgLink("ORG_ID=42&org_name=Acme"))
    }

    @Test
    fun `a referrer without an org is consumed when it is null`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Available(null), calls = calls)

            assertNull(provider.consume())
            assertNull(provider.consume())
            assertEquals(1, calls.size)
            assertTrue(prefs.getBoolean(CONSUMED_KEY, false))
        }

    @Test
    fun `an already consumed referrer is not fetched again`() =
        runTest {
            prefs.edit().putBoolean(CONSUMED_KEY, true).commit()
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Available("org_id=42&org_name=Acme"), calls = calls)

            assertNull(provider.consume())
            assertEquals(0, calls.size)
        }

    @Test
    fun `an org from the referrer marks it consumed`() =
        runTest {
            provider(InstallReferrerResult.Available("org_id=42&org_name=Acme")).consume()

            assertTrue(prefs.getBoolean(CONSUMED_KEY, false))
        }

    @Test
    fun `an unsupported play store marks the referrer consumed`() =
        runTest {
            provider(InstallReferrerResult.Unsupported).consume()

            assertTrue(prefs.getBoolean(CONSUMED_KEY, false))
        }

    @Test
    fun `an unavailable referrer is not marked consumed`() =
        runTest {
            provider(InstallReferrerResult.Unavailable).consume()

            assertFalse(prefs.getBoolean(CONSUMED_KEY, false))
        }

    @Test
    fun `repeated transient failures keep retrying`() =
        runTest {
            val calls = mutableListOf<Int>()
            val provider = provider(InstallReferrerResult.Unavailable, calls = calls)

            repeat(3) { assertNull(provider.consume()) }
            assertEquals(3, calls.size)
        }

    private companion object {
        val CONSUMED_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("install-referrer-consumed")
    }
}