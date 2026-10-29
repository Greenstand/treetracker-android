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
package org.greenstand.android.TreeTracker.devoptions

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RobolectricConfig

@RunWith(RobolectricTestRunner::class)
@RobolectricConfig(manifest = RobolectricConfig.NONE)
class ConfiguratorTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var configurator: Configurator

    private val booleanConfig = BooleanConfig(key = PrefKey("test/boolean"), name = "Boolean", defaultValue = true)
    private val intConfig = IntConfig(key = PrefKey("test/int"), name = "Int", defaultValue = 10)
    private val floatConfig = FloatConfig(key = PrefKey("test/float"), name = "Float", defaultValue = 0.5f)

    @Before
    fun setUp() {
        sharedPreferences =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences("configurator-test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        configurator = Configurator(Preferences(sharedPreferences))
    }

    @Test
    fun `getters return the config default when nothing is stored`() {
        assertTrue(configurator.getBoolean(booleanConfig))
        assertEquals(10, configurator.getInt(intConfig))
        assertEquals(0.5f, configurator.getFloat(floatConfig), 0f)
    }

    @Test
    fun `putBoolean stores the value under the config key`() {
        configurator.putBoolean(booleanConfig, false)

        assertFalse(configurator.getBoolean(booleanConfig))
        assertFalse(sharedPreferences.getBoolean("test/boolean", true))
    }

    @Test
    fun `putInt stores the value under the config key`() {
        configurator.putInt(intConfig, 25)

        assertEquals(25, configurator.getInt(intConfig))
        assertEquals(25, sharedPreferences.getInt("test/int", -1))
    }

    @Test
    fun `putFloat stores the value under the config key`() {
        configurator.putFloat(floatConfig, 2.5f)

        assertEquals(2.5f, configurator.getFloat(floatConfig), 0f)
        assertEquals(2.5f, sharedPreferences.getFloat("test/float", -1f), 0f)
    }

    @Test
    fun `getValue returns the default of each config type`() {
        assertEquals(true, configurator.getValue(booleanConfig))
        assertEquals(10, configurator.getValue(intConfig))
        assertEquals(0.5f, configurator.getValue(floatConfig))
    }

    @Test
    fun `putValue stores a boolean for a boolean config`() {
        configurator.putValue(booleanConfig, false)

        assertEquals(false, configurator.getValue(booleanConfig))
    }

    @Test
    fun `putValue stores an int for an int config`() {
        configurator.putValue(intConfig, 99)

        assertEquals(99, configurator.getValue(intConfig))
    }

    @Test
    fun `putValue stores a float for a float config`() {
        configurator.putValue(floatConfig, 0.125f)

        assertEquals(0.125f, configurator.getValue(floatConfig))
    }

    @Test
    fun `putValue ignores values of unsupported types`() {
        configurator.putValue(intConfig, "99")
        configurator.putValue(floatConfig, 1.5)

        assertEquals(10, configurator.getValue(intConfig))
        assertEquals(0.5f, configurator.getValue(floatConfig))
        assertTrue(sharedPreferences.all.isEmpty())
    }

    @Test
    fun `putValue with a value that does not match the config type throws`() {
        assertThrows(ClassCastException::class.java) {
            configurator.putValue(booleanConfig, 5)
        }
        assertTrue(sharedPreferences.all.isEmpty())
    }

    @Test
    fun `configs with different keys do not affect each other`() {
        val otherIntConfig = intConfig.copy(key = PrefKey("test/other-int"))

        configurator.putInt(intConfig, 1)

        assertEquals(10, configurator.getInt(otherIntConfig))
    }

    @Test
    fun `stored values survive a new configurator over the same preferences`() {
        configurator.putInt(intConfig, 3)

        val reloaded = Configurator(Preferences(sharedPreferences))

        assertEquals(3, reloaded.getInt(intConfig))
    }
}