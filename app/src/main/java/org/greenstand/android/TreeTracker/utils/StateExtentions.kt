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
package org.greenstand.android.TreeTracker.utils

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.updateState(onUpdate: T.() -> T) {
    value = value.onUpdate()
}

inline fun <T> MutableLiveData<T>.updateState(onUpdate: T.() -> T) {
    postValue(onUpdate(value ?: throw IllegalStateException("Must have state")))
}