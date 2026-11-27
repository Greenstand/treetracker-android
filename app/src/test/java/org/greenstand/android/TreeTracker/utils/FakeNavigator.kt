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
package org.greenstand.android.TreeTracker.utils

import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.navigation.Navigator

class FakeClock(
    var now: Long = 0L,
) {
    fun advance(ms: Long) {
        now += ms
    }
}

fun fakeNavigator(
    vararg initialStack: NavKey,
    clock: FakeClock = FakeClock(),
): Navigator = Navigator(mutableListOf(*initialStack), clock = { clock.now })