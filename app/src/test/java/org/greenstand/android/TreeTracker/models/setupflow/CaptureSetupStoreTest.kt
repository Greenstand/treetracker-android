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
package org.greenstand.android.TreeTracker.models.setupflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CaptureSetupStoreTest {
    private lateinit var store: CaptureSetupStore

    @Before
    fun setUp() {
        val sharedPreferences =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences("capture-setup-store-test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        store = CaptureSetupStore(Preferences(sharedPreferences))
    }

    @Test
    fun `load returns null when nothing was saved`() {
        assertNull(store.load())
    }

    @Test
    fun `load returns what was saved`() {
        val setup =
            SavedCaptureSetup(
                userId = 7L,
                destinationWallet = "wallet",
                sessionNote = "note",
                organizationName = "org",
            )

        store.save(setup)

        assertEquals(setup, store.load())
    }

    @Test
    fun `a later save replaces the earlier one, including fields set back to null`() {
        store.save(SavedCaptureSetup(userId = 7L, destinationWallet = "wallet", sessionNote = "note", organizationName = "org"))

        store.save(SavedCaptureSetup(userId = 8L))

        assertEquals(SavedCaptureSetup(userId = 8L), store.load())
    }

    @Test
    fun `clear forgets the saved setup`() {
        store.save(SavedCaptureSetup(userId = 7L, organizationName = "org"))

        store.clear()

        assertNull(store.load())
    }
}
