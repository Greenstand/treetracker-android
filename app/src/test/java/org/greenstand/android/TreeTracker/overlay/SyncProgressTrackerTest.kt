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
package org.greenstand.android.TreeTracker.overlay

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncProgressTrackerTest {
    private val tracker = SyncProgressTracker()

    @Test
    fun `initial state is inactive with every step pending`() {
        val state = tracker.state.value

        assertFalse(state.isActive)
        assertNull(state.currentStep)
        assertNull(state.overallError)
        assertEquals(SyncStep.entries.toList(), state.steps.map { it.step })
        assertTrue(state.steps.all { it == SyncStepState(it.step) })
    }

    @Test
    fun `steps are listed in sync order with display names`() {
        assertEquals(
            listOf("Messages", "Device Config", "Users", "Sessions", "Legacy Trees", "Trees", "Locations"),
            SyncStep.entries.map { it.displayName },
        )
    }

    @Test
    fun `startSync marks the sync active`() {
        tracker.startSync()

        assertTrue(tracker.state.value.isActive)
        assertNull(tracker.state.value.currentStep)
    }

    @Test
    fun `startStep marks only that step running and makes it current`() {
        tracker.startSync()

        tracker.startStep(SyncStep.USERS)

        val state = tracker.state.value
        assertEquals(SyncStep.USERS, state.currentStep)
        assertEquals(StepStatus.RUNNING, state.status(SyncStep.USERS))
        assertTrue(state.steps.filter { it.step != SyncStep.USERS }.all { it.status == StepStatus.PENDING })
    }

    @Test
    fun `updateStepProgress records counts for that step only`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)

        tracker.updateStepProgress(SyncStep.TREES, completed = 3, total = 10)

        val trees = tracker.state.value.stepState(SyncStep.TREES)
        assertEquals(3, trees.itemsCompleted)
        assertEquals(10, trees.itemsTotal)
        assertEquals(StepStatus.RUNNING, trees.status)
        assertEquals(
            0,
            tracker.state.value
                .stepState(SyncStep.LEGACY_TREES)
                .itemsTotal,
        )
    }

    @Test
    fun `completeStep marks the step successful and keeps it current`() {
        tracker.startSync()
        tracker.startStep(SyncStep.MESSAGES)

        tracker.completeStep(SyncStep.MESSAGES)

        assertEquals(StepStatus.SUCCESS, tracker.state.value.status(SyncStep.MESSAGES))
        assertEquals(SyncStep.MESSAGES, tracker.state.value.currentStep)
    }

    @Test
    fun `failStep marks the step as failed with its error`() {
        tracker.startSync()
        tracker.startStep(SyncStep.SESSIONS)

        tracker.failStep(SyncStep.SESSIONS, "offline")

        val sessions = tracker.state.value.stepState(SyncStep.SESSIONS)
        assertEquals(StepStatus.ERROR, sessions.status)
        assertEquals("offline", sessions.errorMessage)
    }

    @Test
    fun `endSync without error deactivates and clears the current step but keeps step results`() {
        tracker.startSync()
        tracker.startStep(SyncStep.MESSAGES)
        tracker.completeStep(SyncStep.MESSAGES)

        tracker.endSync()

        val state = tracker.state.value
        assertFalse(state.isActive)
        assertNull(state.currentStep)
        assertNull(state.overallError)
        assertEquals(StepStatus.SUCCESS, state.status(SyncStep.MESSAGES))
    }

    @Test
    fun `endSync with error records the overall error`() {
        tracker.startSync()

        tracker.endSync(error = "sync failed")

        assertFalse(tracker.state.value.isActive)
        assertEquals("sync failed", tracker.state.value.overallError)
    }

    @Test
    fun `startSync resets results from a previous sync`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)
        tracker.updateStepProgress(SyncStep.TREES, completed = 5, total = 5)
        tracker.failStep(SyncStep.TREES, "boom")
        tracker.endSync(error = "boom")

        tracker.startSync()

        val state = tracker.state.value
        assertTrue(state.isActive)
        assertNull(state.overallError)
        assertNull(state.currentStep)
        assertEquals(SyncStepState(SyncStep.TREES), state.stepState(SyncStep.TREES))
    }

    @Test
    fun `no op tracker never changes its state`() {
        val noOpTracker = NoOpSyncProgressTracker()
        val initial = noOpTracker.state.value

        noOpTracker.startSync()
        noOpTracker.startStep(SyncStep.TREES)
        noOpTracker.updateStepProgress(SyncStep.TREES, completed = 1, total = 2)
        noOpTracker.completeStep(SyncStep.TREES)
        noOpTracker.failStep(SyncStep.LOCATIONS, "boom")
        noOpTracker.endSync(error = "boom")

        assertEquals(initial, noOpTracker.state.value)
        assertEquals(SyncProgressState(), noOpTracker.state.value)
    }

    private fun SyncProgressState.stepState(syncStep: SyncStep): SyncStepState = steps.first { it.step == syncStep }

    private fun SyncProgressState.status(syncStep: SyncStep): StepStatus = stepState(syncStep).status
}