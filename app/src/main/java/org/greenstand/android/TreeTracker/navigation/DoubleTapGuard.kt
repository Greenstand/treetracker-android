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

/**
 * Tells a tap meant for a screen from the second tap of a double tap on the button that opened
 * it. On small screens the next screen's button can sit where the previous screen's button was
 * (Track -> user select's add-user, user select's Next -> Add org's Next), and the origin throttle
 * in [Navigator] lets a first tap from a new screen through.
 *
 * Put [modifier] on the screen's root so it sees every finger down, then check
 * [isSecondTapOfDoubleTap] before acting on a click.
 */
class DoubleTapGuard(
    private val uptimeMillis: () -> Long = SystemClock::uptimeMillis,
    private val doubleTapTimeoutMs: () -> Long = { ViewConfiguration.getDoubleTapTimeout().toLong() },
) {
    private var lastPressUptimeMs: Long? = null

    /** Records the event time of each finger down inside it, without consuming anything. */
    val modifier: Modifier =
        Modifier.pointerInput(this) {
            awaitEachGesture {
                recordPress(awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).uptimeMillis)
            }
        }

    internal fun recordPress(uptimeMs: Long) {
        lastPressUptimeMs = uptimeMs
    }

    /**
     * Whether the latest press is the second half of a double tap that navigated to this screen.
     * Each check uses up the recorded press, so an action with no press of its own (the keyboard's
     * Go) is judged as happening now rather than by an older tap on the screen.
     */
    fun isSecondTapOfDoubleTap(navigator: Navigator): Boolean {
        val millisSincePress = lastPressUptimeMs?.let { uptimeMillis() - it } ?: 0L
        lastPressUptimeMs = null
        return isSecondTapOfDoubleTap(navigator.millisSinceLastNavigation, millisSincePress, doubleTapTimeoutMs())
    }
}

@Composable
fun rememberDoubleTapGuard(): DoubleTapGuard = remember { DoubleTapGuard() }

/**
 * Whether a press that went down [millisSincePress] ago, on a screen whose navigator last changed
 * the stack [millisSinceLastNavigation] ago, is the second half of a double tap on the button
 * that navigated here: it went down within the system double-tap timeout of that navigation (or
 * before it). Uses the press's own event time, so a double tap that reaches the app late (a busy
 * device) is still caught.
 */
internal fun isSecondTapOfDoubleTap(
    millisSinceLastNavigation: Long,
    millisSincePress: Long,
    doubleTapTimeoutMs: Long = ViewConfiguration.getDoubleTapTimeout().toLong(),
): Boolean {
    if (millisSinceLastNavigation == Long.MAX_VALUE) return false
    return millisSinceLastNavigation - millisSincePress.coerceAtLeast(0) < doubleTapTimeoutMs
}