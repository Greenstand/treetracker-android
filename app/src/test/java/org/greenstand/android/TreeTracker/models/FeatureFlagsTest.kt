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
package org.greenstand.android.TreeTracker.models

import org.greenstand.android.TreeTracker.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFlagsTest {
    @Test
    fun `debug is enabled only for dev and debug builds`() {
        val expected = BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "debug"

        assertEquals(expected, FeatureFlags.DEBUG_ENABLED)
    }

    @Test
    fun `beta is enabled only for beta builds`() {
        assertEquals(BuildConfig.BUILD_TYPE == "beta", FeatureFlags.BETA)
    }

    @Test
    fun `feature flags mirror the build config`() {
        assertEquals(BuildConfig.TREE_HEIGHT_FEATURE_ENABLED, FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED)
        assertEquals(BuildConfig.TREE_NOTE_FEATURE_ENABLED, FeatureFlags.TREE_NOTE_FEATURE_ENABLED)
        assertEquals(BuildConfig.TREE_DBH_FEATURE_ENABLED, FeatureFlags.TREE_DBH_FEATURE_ENABLED)
        assertEquals(
            BuildConfig.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED,
            FeatureFlags.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED,
        )
        assertEquals(BuildConfig.BLUR_DETECTION_ENABLED, FeatureFlags.BLUR_DETECTION_ENABLED)
        assertEquals(BuildConfig.ORG_LINK, FeatureFlags.ORG_LINK_ENABLED)
    }
}