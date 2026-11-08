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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `WHEN NavigateBack action THEN emits PopBackStackEvent`() =
        runTest {
            every { configurator.getBoolean(any()) } returns false
            every { configurator.getInt(any()) } returns 100
            every { configurator.getFloat(any()) } returns 0.5f

            val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
            viewModel.handleAction(DevOptionsAction.NavigateBack)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
        }

    private fun stubStoredValues() {
        every { configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE) } returns true
        every { configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT) } returns 1280
        every { configurator.getInt(ConfigKeys.CONVERGENCE_TIMEOUT) } returns 15000
        every { configurator.getInt(ConfigKeys.CONVERGENCE_DATA_SIZE) } returns 7
        every { configurator.getFloat(ConfigKeys.LAT_STD_DEV_THRESHOLD) } returns 0.25f
        every { configurator.getFloat(ConfigKeys.LON_STD_DEV_THRESHOLD) } returns 0.5f
    }

    private inline fun <reified T : Config> DevOptionsViewModel.param(key: Config): T =
        state.value.params
            .filterIsInstance<T>()
            .single { it.key == key.key }

    @Test
    fun `WHEN init THEN each config keeps its order and shows the stored value`() {
        stubStoredValues()

        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)

        assertEquals(
            listOf(
                ConfigKeys.FORCE_IMAGE_SIZE.copy(defaultValue = true),
                ConfigKeys.IMAGE_CAPTURE_HEIGHT.copy(defaultValue = 1280),
                ConfigKeys.CONVERGENCE_TIMEOUT.copy(defaultValue = 15000),
                ConfigKeys.CONVERGENCE_DATA_SIZE.copy(defaultValue = 7),
                ConfigKeys.LAT_STD_DEV_THRESHOLD.copy(defaultValue = 0.25f),
                ConfigKeys.LON_STD_DEV_THRESHOLD.copy(defaultValue = 0.5f),
            ),
            viewModel.state.value.params,
        )
        assertTrue(viewModel.events.replayCache.isEmpty())
    }

    @Test
    fun `WHEN updateParam called with IntConfig THEN only that param changes`() {
        stubStoredValues()
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
        val heightParam = viewModel.param<IntConfig>(ConfigKeys.IMAGE_CAPTURE_HEIGHT)
        val before = viewModel.state.value.params

        viewModel.handleAction(DevOptionsAction.UpdateParam(heightParam, 2048))

        verify(exactly = 1) { configurator.putValue(heightParam, 2048) }
        val after = viewModel.state.value.params
        assertEquals(2048, viewModel.param<IntConfig>(ConfigKeys.IMAGE_CAPTURE_HEIGHT).defaultValue)
        assertEquals(before.size, after.size)
        assertEquals(before.filterIndexed { i, _ -> i != 1 }, after.filterIndexed { i, _ -> i != 1 })
        assertEquals(ConfigKeys.IMAGE_CAPTURE_HEIGHT.key, after[1].key)
    }

    @Test
    fun `WHEN updateParam called with FloatConfig THEN configurator and state are updated`() {
        stubStoredValues()
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
        val latParam = viewModel.param<FloatConfig>(ConfigKeys.LAT_STD_DEV_THRESHOLD)

        viewModel.handleAction(DevOptionsAction.UpdateParam(latParam, 0.75f))

        verify(exactly = 1) { configurator.putValue(latParam, 0.75f) }
        assertEquals(0.75f, viewModel.param<FloatConfig>(ConfigKeys.LAT_STD_DEV_THRESHOLD).defaultValue)
    }

    @Test
    fun `WHEN BooleanConfig is toggled twice THEN state follows the latest value`() {
        stubStoredValues()
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)

        val first = viewModel.param<BooleanConfig>(ConfigKeys.FORCE_IMAGE_SIZE)
        viewModel.handleAction(DevOptionsAction.UpdateParam(first, false))
        val second = viewModel.param<BooleanConfig>(ConfigKeys.FORCE_IMAGE_SIZE)
        viewModel.handleAction(DevOptionsAction.UpdateParam(second, true))

        verify(exactly = 1) { configurator.putValue(first, false) }
        verify(exactly = 1) { configurator.putValue(second, true) }
        assertTrue(viewModel.param<BooleanConfig>(ConfigKeys.FORCE_IMAGE_SIZE).defaultValue)
    }

    @Test
    fun `WHEN NavigateBack THEN pops dev options without saving anything`() {
        stubStoredValues()
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
        val navigator = fakeNavigator(DashboardRoute, SettingsRoute, DevOptionsRoute)

        viewModel.handleAction(DevOptionsAction.NavigateBack)

        assertEquals(
            PopBackStackEvent,
            viewModel.events.replayCache
                .single()
                .getContentIfNotConsumed(),
        )
        // HandleUIEvents dispatches PopBackStackEvent as a throttled pop.
        navigator.throttledPopBackStack()
        assertEquals(listOf(DashboardRoute, SettingsRoute), navigator.backStack)
        verify(exactly = 0) { configurator.putValue(any(), any()) }
    }

    @Test
    fun `WHEN view model is cleared THEN convergence configuration is refreshed`() {
        stubStoredValues()
        val viewModel = DevOptionsViewModel(configurator, convergenceConfiguration)
        val store = ViewModelStore()
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
            }
        ViewModelProvider(store, factory)[DevOptionsViewModel::class.java]
        verify(exactly = 0) { convergenceConfiguration.refreshConfig() }

        store.clear()

        verify(exactly = 1) { convergenceConfiguration.refreshConfig() }
    }

    @Test
    fun `updateListItem replaces only the matching item`() {
        val updated = listOf("a", "b", "c").updateListItem("b") { "B" }

        assertEquals(listOf("a", "B", "c"), updated)
    }

    @Test
    fun `updateListItem does not modify the original list`() {
        val original = listOf(1, 2, 3)

        original.updateListItem(2) { 20 }

        assertEquals(listOf(1, 2, 3), original)
        assertFalse(original.contains(20))
    }
}