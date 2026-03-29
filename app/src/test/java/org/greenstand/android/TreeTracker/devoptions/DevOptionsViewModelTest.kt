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
package org.greenstand.android.TreeTracker.devoptions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DevOptionsViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var configurator: Configurator

    @MockK(relaxed = true)
    private lateinit var convergenceConfiguration: ConvergenceConfiguration

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN init THEN loads config params from configurator`() =
        runTest {
            every { configurator.getBoolean(any()) } returns false
            every { configurator.getInt(any()) } returns 100
            every { configurator.getFloat(any()) } returns 0.5f

            val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)

            val state = viewModel.state.first()
            assertEquals(ConfigKeys.configList.size, state.params.size)
        }

    @Test
    fun `WHEN updateParam called with BooleanConfig THEN updates configurator and state`() =
        runTest {
            every { configurator.getBoolean(any()) } returns false
            every { configurator.getInt(any()) } returns 100
            every { configurator.getFloat(any()) } returns 0.5f

            val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)

            val booleanParam = ConfigKeys.FORCE_IMAGE_SIZE
            viewModel.handleAction(DevOptionsAction.UpdateParam(booleanParam, true))

            verify { configurator.putValue(booleanParam, true) }

            val state = viewModel.state.first()
            val updatedParam =
                state.params
                    .filterIsInstance<BooleanConfig>()
                    .find { it.key == booleanParam.key }
            assertTrue(updatedParam!!.defaultValue)
        }
}