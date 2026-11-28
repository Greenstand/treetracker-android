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
package org.greenstand.android.TreeTracker.e2e

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Debug-only stand-in for S3, so e2e flows can sync successfully without a network or real keys.
 * Every upload succeeds, after an optional delay that keeps a sync running long enough to stop it.
 * Installed by the `storage` parameter of a `treetracker-e2e://` link; it lasts until the app
 * process dies.
 */
object FakeObjectStorage {
    private const val DEFAULT_SLOW_DELAY_MS = 1000L

    fun install(
        client: ObjectStorageClient,
        mode: String,
        delayMs: Long?,
    ) {
        client.s3Client = create(delayFor(mode, delayMs))
    }

    internal fun delayFor(
        mode: String,
        delayMs: Long?,
    ): Long =
        when (mode) {
            "ok" -> 0L
            "slow" -> delayMs ?: DEFAULT_SLOW_DELAY_MS
            else -> throw IllegalArgumentException("Unknown storage mode '$mode', use ok or slow")
        }

    // Uploads only ever call putObject, so that is all the fake answers; anything else fails loudly.
    internal fun create(delayMs: Long): AmazonS3 =
        Proxy.newProxyInstance(
            AmazonS3::class.java.classLoader,
            arrayOf(AmazonS3::class.java),
            InvocationHandler { proxy, method, args ->
                when {
                    method.name == "putObject" -> {
                        if (delayMs > 0) Thread.sleep(delayMs)
                        PutObjectResult()
                    }
                    method.declaringClass == Any::class.java ->
                        when (method.name) {
                            "hashCode" -> System.identityHashCode(proxy)
                            "equals" -> proxy === args?.firstOrNull()
                            else -> "FakeObjectStorage"
                        }
                    else -> throw UnsupportedOperationException("FakeObjectStorage does not support ${method.name}")
                }
            },
        ) as AmazonS3
}