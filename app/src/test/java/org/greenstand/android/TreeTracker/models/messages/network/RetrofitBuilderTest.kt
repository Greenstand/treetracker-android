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
package org.greenstand.android.TreeTracker.models.messages.network

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class RetrofitBuilderTest {
    private val json = Json { ignoreUnknownKeys = true }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `create points retrofit at the api gateway`() {
        val retrofit = RetrofitBuilder(json).create()

        assertEquals(BuildConfig.API_GATEWAY.toHttpUrl(), retrofit.baseUrl())
    }

    @Test
    fun `create logs request bodies when debug is enabled`() {
        mockkObject(FeatureFlags)
        every { FeatureFlags.DEBUG_ENABLED } returns true

        val retrofit = RetrofitBuilder(json).create()

        assertEquals(HttpLoggingInterceptor.Level.BODY, loggingInterceptor(retrofit.callFactory()).level)
    }

    @Test
    fun `create disables logging when debug is disabled`() {
        mockkObject(FeatureFlags)
        every { FeatureFlags.DEBUG_ENABLED } returns false

        val retrofit = RetrofitBuilder(json).create()

        assertEquals(HttpLoggingInterceptor.Level.NONE, loggingInterceptor(retrofit.callFactory()).level)
    }

    private fun loggingInterceptor(callFactory: okhttp3.Call.Factory): HttpLoggingInterceptor =
        (callFactory as OkHttpClient)
            .interceptors
            .filterIsInstance<HttpLoggingInterceptor>()
            .single()
}