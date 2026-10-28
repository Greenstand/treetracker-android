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
package org.greenstand.android.TreeTracker.root

import org.greenstand.android.TreeTracker.di.featureModules
import org.greenstand.android.TreeTracker.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.compose.navigation3.EntryProviderInstaller
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.TypeQualifier
import org.koin.dsl.koinApplication

/**
 * Features register their screens with Koin, so a route with no screen would only fail when
 * someone navigates to it. These tests catch that, and two features registering the same route,
 * at build time.
 */
@OptIn(KoinExperimentalAPI::class)
class ScreenRegistrationTest {
    @Test
    fun `every route has a registered screen`() {
        val koin = koinApplication { modules(featureModules) }.koin
        val missing =
            Route::class
                .sealedSubclasses
                .filter { koin.getOrNull<EntryProviderInstaller>(TypeQualifier(it)) == null }
                .map { it.simpleName }

        assertEquals("Routes with no screen registered", emptyList<String>(), missing)
    }

    @Test
    fun `no route or ViewModel is registered twice`() {
        // Throws DefinitionOverrideException if two feature modules define the same thing.
        koinApplication {
            allowOverride(false)
            modules(featureModules)
        }
    }
}