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
package org.greenstand.android.TreeTracker.preferences

open class PrefKey(val path: String) {

    operator fun plus(prefKey: PrefKey): PrefKey {
        return when (prefKey) {
            is UserPrefKey -> UserPrefKey("$path/${prefKey.path}")
            else -> PrefKey("$path/${prefKey.path}")
        }
    }

    fun asUserPref(): UserPrefKey {
        return UserPrefKey(path)
    }
}