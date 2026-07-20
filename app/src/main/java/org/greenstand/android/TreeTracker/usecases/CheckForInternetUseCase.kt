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
package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.CrashKey
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector

/**
 *  checks if the internet is available on the user's device
 */
class CheckForInternetUseCase(
    private val exceptionDataCollector: ExceptionDataCollector,
) : UseCase<Unit, Boolean>() {
    override suspend fun execute(params: Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val command = "ping -c 1 google.com"
                val isOnline = Runtime.getRuntime().exec(command).waitFor() == 0
                exceptionDataCollector.set(CrashKey.IS_ONLINE, true)
                isOnline
            } catch (e: Exception) {
                exceptionDataCollector.set(CrashKey.IS_ONLINE, false)
                false
            }
        }
}