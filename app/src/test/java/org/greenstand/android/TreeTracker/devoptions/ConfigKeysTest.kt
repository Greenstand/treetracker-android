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

import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.UserPrefKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConfigKeysTest {
    @Test
    fun `every config key lives under the system settings`() {
        assertEquals(
            listOf(
                "greenstand/system-settings/force-image-size",
                "greenstand/system-settings/image-capture-height",
                "greenstand/system-settings/convergence-timeout",
                "greenstand/system-settings/convergence-data-size",
                "greenstand/system-settings/lat-std-dev-threshold",
                "greenstand/system-settings/lon-std-dev-threshold",
            ),
            ConfigKeys.configList.map { it.key.path },
        )
    }

    @Test
    fun `config keys are shared by all users`() {
        ConfigKeys.configList.forEach { config -> assertFalse(config.name, config.key is UserPrefKey) }
    }

    @Test
    fun `configList lists every config once in display order`() {
        assertEquals(
            listOf(
                ConfigKeys.FORCE_IMAGE_SIZE,
                ConfigKeys.IMAGE_CAPTURE_HEIGHT,
                ConfigKeys.CONVERGENCE_TIMEOUT,
                ConfigKeys.CONVERGENCE_DATA_SIZE,
                ConfigKeys.LAT_STD_DEV_THRESHOLD,
                ConfigKeys.LON_STD_DEV_THRESHOLD,
            ),
            ConfigKeys.configList,
        )
        assertEquals(
            ConfigKeys.configList.size,
            ConfigKeys.configList
                .map { it.key.path }
                .toSet()
                .size,
        )
    }

    @Test
    fun `force image size is a boolean config that defaults to off`() {
        val config = ConfigKeys.FORCE_IMAGE_SIZE

        assertEquals("Force Image Size", config.name)
        assertFalse(config.defaultValue)
    }

    @Test
    fun `image capture height is an int config that defaults to 1920`() {
        val config = ConfigKeys.IMAGE_CAPTURE_HEIGHT

        assertEquals("Image Capture Height", config.name)
        assertEquals(1920, config.defaultValue)
    }

    @Test
    fun `convergence data size is an int config that defaults to 5`() {
        val config = ConfigKeys.CONVERGENCE_DATA_SIZE

        assertEquals("Convergence Data Size", config.name)
        assertEquals(5, config.defaultValue)
    }

    @Test
    fun `convergence timeout is an int config that defaults to 20 seconds`() {
        val config = ConfigKeys.CONVERGENCE_TIMEOUT

        assertEquals("Convergence Timeout", config.name)
        assertEquals(20000, config.defaultValue)
    }

    @Test
    fun `std dev thresholds are float configs that default to 0_00001`() {
        assertEquals("Lat Std Dev Threshold", ConfigKeys.LAT_STD_DEV_THRESHOLD.name)
        assertEquals(0.00001f, ConfigKeys.LAT_STD_DEV_THRESHOLD.defaultValue, 0f)
        assertEquals("Lon Std Dev Threshold", ConfigKeys.LON_STD_DEV_THRESHOLD.name)
        assertEquals(0.00001f, ConfigKeys.LON_STD_DEV_THRESHOLD.defaultValue, 0f)
    }

    @Test
    fun `configs are data classes compared by value`() {
        val key = PrefKey("a")

        assertEquals(IntConfig(key, "n", 1), IntConfig(key, "n", 1))
        assertNotEquals(IntConfig(key, "n", 1), IntConfig(key, "n", 2))
        assertEquals(BooleanConfig(key, "n", true), BooleanConfig(key, "n", true))
        assertEquals(FloatConfig(key, "n", 1f), FloatConfig(key, "n", 1f))
    }
}