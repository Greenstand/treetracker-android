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
package org.greenstand.android.TreeTracker.navigation

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

class DoubleTapGuard(
    private val uptimeMillis: () -> Long = SystemClock::uptimeMillis,
    private val doubleTapTimeoutMs: () -> Long = { ViewConfiguration.getDoubleTapTimeout().toLong() },
) {
    private var lastPressUptimeMs: Long? = null

    val modifier: Modifier =
        Modifier.pointerInput(this) {
            awaitEachGesture {
                recordPress(awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).uptimeMillis)
            }
        }

    internal fun recordPress(uptimeMs: Long) {
        lastPressUptimeMs = uptimeMs
    }

    fun isSecondTapOfDoubleTap(navigator: Navigator): Boolean {
        val millisSincePress = lastPressUptimeMs?.let { uptimeMillis() - it } ?: 0L
        lastPressUptimeMs = null
        return isSecondTapOfDoubleTap(navigator.millisSinceLastNavigation, millisSincePress, doubleTapTimeoutMs())
    }
}

@Composable
fun rememberDoubleTapGuard(): DoubleTapGuard = remember { DoubleTapGuard() }

internal fun isSecondTapOfDoubleTap(
    millisSinceLastNavigation: Long,
    millisSincePress: Long,
    doubleTapTimeoutMs: Long = ViewConfiguration.getDoubleTapTimeout().toLong(),
): Boolean {
    if (millisSinceLastNavigation == Long.MAX_VALUE) return false
    return millisSinceLastNavigation - millisSincePress.coerceAtLeast(0) < doubleTapTimeoutMs
}