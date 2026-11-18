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
package org.greenstand.android.TreeTracker.models.captureflowdata

import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope
import timber.log.Timber

object CaptureFlowScopeManager {
    private var currentScope: Scope? = null

    val isOpen: Boolean get() = currentScope != null

    fun open() {
        if (currentScope != null) {
            Timber.tag("CaptureFlowScope").w("Scope already open, closing previous before reopening")
            close()
        }
        currentScope = CaptureFlowScope().scope
        currentScope?.get<CaptureFlowScope>()
    }

    /**
     * Reopens the scope when Android restores a capture step ([route], after the photo was taken)
     * after killing the app, so the tree whose photo was taken can still be finished and saved.
     * Does nothing while the scope is open. Returns true when it reopened the scope.
     *
     * When the kept tree can't be picked up (none kept, no session to resume, the tree belongs to
     * another session, or [route] isn't in the flow) the scope is still opened, so the screen can
     * be built, but the kept tree is dropped and `TreeCapturer.currentTree` stays null. The screen
     * then sends the user back to the dashboard.
     */
    fun restoreAt(route: String): Boolean {
        if (isOpen) return false
        Timber.tag("CaptureFlowScope").i("Restoring capture flow at $route after process death")
        open()
        val treeCapturer = getData().scope.get<TreeCapturer>()
        val tree = treeCapturer.restorePendingTree()
        if (tree == null || !nav.resumeAt(route, tree.sessionId)) {
            Timber.tag("CaptureFlowScope").w("Nothing to restore at $route, dropping the kept tree")
            treeCapturer.discardPendingTree()
        }
        return true
    }

    fun getData(): CaptureFlowScope =
        currentScope?.get()
            ?: error("CaptureFlowScope not open. Call open() before getData().")

    val nav: CaptureFlowNavigationController
        get() =
            currentScope?.get()
                ?: error("CaptureFlowScope not open. Call open() before accessing nav.")

    fun close() {
        currentScope?.close()
        currentScope = null
    }
}

class CaptureFlowScope : KoinScopeComponent {
    override val scope: Scope by lazy {
        createScope(this)
    }
}