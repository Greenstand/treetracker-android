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
package org.greenstand.android.TreeTracker.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefKeyTest {
    @Test
    fun `a pref key keeps its path`() {
        assertEquals("some/path", PrefKey("some/path").path)
    }

    @Test
    fun `adding a plain key joins the paths with a slash`() {
        val key = PrefKey("parent") + PrefKey("child")

        assertEquals("parent/child", key.path)
        assertFalse(key is UserPrefKey)
    }

    @Test
    fun `adding a user key produces a user key`() {
        val key = PrefKey("parent") + UserPrefKey("child")

        assertTrue(key is UserPrefKey)
        assertEquals("parent/child", key.path)
    }

    @Test
    fun `keys can be chained`() {
        val key = PrefKey("a") + PrefKey("b") + UserPrefKey("c")

        assertEquals("a/b/c", key.path)
        assertTrue(key is UserPrefKey)
    }

    @Test
    fun `asUserPref turns a key into a user key with the same path`() {
        val userKey: PrefKey = PrefKey("parent/child").asUserPref()

        assertTrue(userKey is UserPrefKey)
        assertEquals("parent/child", userKey.path)
    }

    @Test
    fun `a user key is a pref key`() {
        val key: PrefKey = UserPrefKey("user/key")

        assertEquals("user/key", key.path)
    }

    @Test
    fun `root pref keys live under the greenstand root`() {
        assertEquals("greenstand/session", PrefKeys.SESSION.path)
        assertEquals("greenstand/user-settings", PrefKeys.USER_SETTINGS.path)
        assertEquals("greenstand/system-settings", PrefKeys.SYSTEM_SETTINGS.path)
    }

    @Test
    fun `root pref keys are not user keys`() {
        assertFalse(PrefKeys.SESSION is UserPrefKey)
        assertFalse(PrefKeys.USER_SETTINGS is UserPrefKey)
        assertFalse(PrefKeys.SYSTEM_SETTINGS is UserPrefKey)
    }
}