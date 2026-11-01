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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PreferencesTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferences: Preferences

    private val key = PrefKeys.SYSTEM_SETTINGS + PrefKey("value")
    private val otherKey = PrefKeys.SYSTEM_SETTINGS + PrefKey("other")
    private val userKey = PrefKeys.USER_SETTINGS + UserPrefKey("value")

    @Before
    fun setUp() {
        sharedPreferences =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences("preferences-test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        preferences = Preferences(sharedPreferences)
    }

    // region getters and defaults

    @Test
    fun `getters return their built-in defaults for missing keys`() {
        assertNull(preferences.getString(key))
        assertEquals(-1L, preferences.getLong(key))
        assertEquals(-1f, preferences.getFloat(key), 0f)
        assertEquals(-1, preferences.getInt(key))
    }

    @Test
    fun `getters return the given default for missing keys`() {
        assertTrue(preferences.getBoolean(key, true))
        assertFalse(preferences.getBoolean(key, false))
        assertEquals("fallback", preferences.getString(key, "fallback"))
        assertEquals(7L, preferences.getLong(key, 7L))
        assertEquals(1.5f, preferences.getFloat(key, 1.5f), 0f)
        assertEquals(3, preferences.getInt(key, 3))
    }

    @Test
    fun `putBoolean then getBoolean returns the stored value`() {
        preferences.edit().putBoolean(key, true).apply()

        assertTrue(preferences.getBoolean(key, false))
    }

    @Test
    fun `putString then getString returns the stored value`() {
        preferences.edit().putString(key, "hello").apply()

        assertEquals("hello", preferences.getString(key))
        assertEquals("hello", preferences.getString(key, "fallback"))
    }

    @Test
    fun `putString with null removes the value`() {
        preferences.edit().putString(key, "hello").apply()

        preferences.edit().putString(key, null).apply()

        assertEquals("fallback", preferences.getString(key, "fallback"))
        assertFalse(sharedPreferences.contains(key.path))
    }

    @Test
    fun `putLong then getLong returns the stored value`() {
        preferences.edit().putLong(key, 1_700_000_000_000L).apply()

        assertEquals(1_700_000_000_000L, preferences.getLong(key))
    }

    @Test
    fun `putFloat then getFloat returns the stored value`() {
        preferences.edit().putFloat(key, 0.25f).apply()

        assertEquals(0.25f, preferences.getFloat(key), 0f)
    }

    @Test
    fun `putInt then getInt returns the stored value`() {
        preferences.edit().putInt(key, 42).apply()

        assertEquals(42, preferences.getInt(key))
    }

    @Test
    fun `editor chains several puts and commit stores them all`() {
        preferences
            .edit()
            .putBoolean(key, true)
            .putInt(otherKey, 9)
            .commit()

        assertTrue(preferences.getBoolean(key, false))
        assertEquals(9, preferences.getInt(otherKey))
    }

    @Test
    fun `values are not stored until the editor is applied`() {
        val editor = preferences.edit().putInt(key, 5)

        assertEquals(-1, preferences.getInt(key))

        editor.apply()
        assertEquals(5, preferences.getInt(key))
    }

    @Test
    fun `remove deletes the value`() {
        preferences
            .edit()
            .putInt(key, 5)
            .putInt(otherKey, 6)
            .apply()

        preferences.edit().remove(key).apply()

        assertEquals(-1, preferences.getInt(key))
        assertEquals(6, preferences.getInt(otherKey))
    }

    // endregion

    // region key paths

    @Test
    fun `a plain key is stored under its path`() {
        preferences.edit().putInt(key, 5).apply()

        assertEquals(5, sharedPreferences.getInt("greenstand/system-settings/value", -1))
    }

    @Test
    fun `values written directly under a key path are read back`() {
        sharedPreferences.edit().putString("greenstand/system-settings/value", "raw").commit()

        assertEquals("raw", preferences.getString(key))
    }

    @Test
    fun `a user key is stored under its path plus the current user id`() {
        preferences.setUserId(12)

        preferences.edit().putInt(userKey, 5).apply()

        assertEquals(5, sharedPreferences.getInt("greenstand/user-settings/value/12", -1))
        assertFalse(sharedPreferences.contains("greenstand/user-settings/value"))
    }

    @Test
    fun `a user key without a user id is stored under null`() {
        preferences.edit().putInt(userKey, 5).apply()

        assertEquals(5, sharedPreferences.getInt("greenstand/user-settings/value/null", -1))
    }

    @Test
    fun `user keys keep separate values per user`() {
        preferences.setUserId(1)
        preferences.edit().putString(userKey, "first").apply()
        preferences.setUserId(2)
        preferences.edit().putString(userKey, "second").apply()

        assertEquals("second", preferences.getString(userKey))
        preferences.setUserId(1)
        assertEquals("first", preferences.getString(userKey))
        preferences.setUserId(3)
        assertNull(preferences.getString(userKey))
    }

    @Test
    fun `clearing the user id reads user keys from the null slot`() {
        preferences.setUserId(1)
        preferences.edit().putInt(userKey, 5).apply()

        preferences.setUserId(null)

        assertEquals(-1, preferences.getInt(userKey))
    }

    @Test
    fun `plain keys are shared between users`() {
        preferences.setUserId(1)
        preferences.edit().putInt(key, 5).apply()

        preferences.setUserId(2)

        assertEquals(5, preferences.getInt(key))
    }

    @Test
    fun `the editor resolves user keys with the user id at the time of the put`() {
        val editor = preferences.edit()
        preferences.setUserId(1)
        editor.putInt(userKey, 5)
        preferences.setUserId(2)

        editor.apply()

        assertEquals(5, sharedPreferences.getInt("greenstand/user-settings/value/1", -1))
        assertFalse(sharedPreferences.contains("greenstand/user-settings/value/2"))
    }

    @Test
    fun `remove on a user key removes only the current user's value`() {
        preferences.setUserId(1)
        preferences.edit().putInt(userKey, 1).apply()
        preferences.setUserId(2)
        preferences.edit().putInt(userKey, 2).apply()

        preferences.edit().remove(userKey).apply()

        assertEquals(-1, preferences.getInt(userKey))
        preferences.setUserId(1)
        assertEquals(1, preferences.getInt(userKey))
    }

    // endregion

    // region clearSessionData

    @Test
    fun `clearSessionData removes every session value including user scoped ones`() {
        preferences.setUserId(4)
        val sessionKey = PrefKeys.SESSION + PrefKey("steps")
        val sessionUserKey = PrefKeys.SESSION + UserPrefKey("note")
        preferences
            .edit()
            .putInt(sessionKey, 10)
            .putString(sessionUserKey, "note")
            .apply()

        preferences.clearSessionData()

        assertEquals(-1, preferences.getInt(sessionKey))
        assertNull(preferences.getString(sessionUserKey))
        assertTrue(sharedPreferences.all.keys.none { it.startsWith("greenstand/session") })
    }

    @Test
    fun `clearSessionData keeps settings outside the session`() {
        preferences.setUserId(4)
        preferences
            .edit()
            .putInt(PrefKeys.SESSION + PrefKey("steps"), 10)
            .putInt(key, 1)
            .putString(userKey, "user")
            .apply()
        sharedPreferences.edit().putBoolean("unrelated", true).commit()

        preferences.clearSessionData()

        assertEquals(1, preferences.getInt(key))
        assertEquals("user", preferences.getString(userKey))
        assertTrue(sharedPreferences.getBoolean("unrelated", false))
    }

    @Test
    fun `clearSessionData with nothing stored leaves preferences empty`() {
        preferences.clearSessionData()

        assertTrue(sharedPreferences.all.isEmpty())
    }

    // endregion

    // region observe

    @Test
    fun `observeInt emits the new value when the key changes`() =
        runTest {
            preferences.observeInt(key).test {
                expectNoEvents()

                preferences.edit().putInt(key, 3).apply()
                assertEquals(3, awaitItem())

                preferences.edit().putInt(key, 4).apply()
                assertEquals(4, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeInt ignores changes to other keys`() =
        runTest {
            preferences.observeInt(key).test {
                preferences.edit().putInt(otherKey, 3).apply()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeInt emits the default when the value is removed`() =
        runTest {
            preferences.edit().putInt(key, 3).apply()

            preferences.observeInt(key, default = 11).test {
                assertEquals(3, awaitItem())

                preferences.edit().remove(key).apply()
                assertEquals(11, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeInt replays the latest change to a new collector`() =
        runTest {
            preferences.edit().putInt(key, 8).apply()

            assertEquals(8, preferences.observeInt(key).first())
        }

    @Test
    fun `observeInt does not replay when the latest change was another key`() =
        runTest {
            preferences.edit().putInt(key, 8).apply()
            preferences.edit().putInt(otherKey, 9).apply()

            preferences.observeInt(key).test {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeInt on a user key follows the current user's value`() =
        runTest {
            preferences.setUserId(5)

            preferences.observeInt(userKey).test {
                preferences.edit().putInt(userKey, 21).apply()
                assertEquals(21, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeString emits the new value when the key changes`() =
        runTest {
            preferences.observeString(key).test {
                expectNoEvents()

                preferences.edit().putString(key, "a").apply()
                assertEquals("a", awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeString emits the default when the value is removed`() =
        runTest {
            preferences.edit().putString(key, "a").apply()

            preferences.observeString(key, default = "gone").test {
                assertEquals("a", awaitItem())

                preferences.edit().remove(key).apply()
                assertEquals("gone", awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeString ignores changes to other keys`() =
        runTest {
            preferences.observeString(key).test {
                preferences.edit().putString(otherKey, "b").apply()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion
}