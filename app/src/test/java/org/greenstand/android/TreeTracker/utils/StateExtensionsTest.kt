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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StateExtensionsTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private data class CounterState(
        val count: Int = 0,
        val label: String = "",
    )

    @Test
    fun `MutableStateFlow updateState replaces the value with the updated copy`() {
        val state = MutableStateFlow(CounterState(count = 1, label = "a"))

        state.updateState { copy(count = count + 1) }

        assertEquals(CounterState(count = 2, label = "a"), state.value)
    }

    @Test
    fun `MutableStateFlow updateState applies successive updates`() {
        val state = MutableStateFlow(CounterState())

        state.updateState { copy(count = 5) }
        state.updateState { copy(label = "done") }

        assertEquals(CounterState(count = 5, label = "done"), state.value)
    }

    @Test
    fun `MutableLiveData updateState posts the updated copy`() {
        val state = MutableLiveData(CounterState(count = 1))

        state.updateState { copy(count = count * 10) }

        assertEquals(CounterState(count = 10), state.value)
    }

    @Test(expected = IllegalStateException::class)
    fun `MutableLiveData updateState without a value throws`() {
        val state = MutableLiveData<CounterState>()

        state.updateState { copy(count = 1) }
    }
}