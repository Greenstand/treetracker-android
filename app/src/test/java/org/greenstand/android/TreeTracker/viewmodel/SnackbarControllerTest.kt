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
package org.greenstand.android.TreeTracker.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SnackbarControllerTest {
    private val controller = SnackbarController()

    @Test
    fun `show delivers the snackbar to a subscriber`() =
        runTest {
            val snackbar = ShowSnackbar(TextRef("Saved"))

            controller.events.test {
                controller.show(snackbar)

                assertEquals(snackbar, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `show delivers several snackbars in order`() =
        runTest {
            controller.events.test {
                controller.show(ShowSnackbar(TextRef("one")))
                controller.show(ShowSnackbar(TextRef("two")))
                controller.show(ShowSnackbar(TextRef("three")))

                assertEquals(TextRef("one"), awaitItem().message)
                assertEquals(TextRef("two"), awaitItem().message)
                assertEquals(TextRef("three"), awaitItem().message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `show without a subscriber does not throw and is not replayed later`() =
        runTest {
            controller.show(ShowSnackbar(TextRef("missed")))

            controller.events.test {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `every subscriber receives the snackbar`() =
        runTest {
            val snackbar = ShowSnackbar(TextRef("Shared"))

            controller.events.test {
                val first = this
                controller.events.test {
                    controller.show(snackbar)

                    assertEquals(snackbar, awaitItem())
                    assertEquals(snackbar, first.awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
}