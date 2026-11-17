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
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
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
     * Reopens the scope when Android restores image review after killing the app, so the tree
     * whose photo was taken can still be reviewed and saved. Does nothing while the scope is open.
     * Returns true when it restored the flow.
     */
    fun restoreOnImageReview(): Boolean {
        if (isOpen) return false
        Timber.tag("CaptureFlowScope").i("Restoring capture flow on image review after process death")
        open()
        val data = getData()
        if (data.scope.get<TreeCapturer>().restorePendingTree() == null) {
            Timber.tag("CaptureFlowScope").w("No pending tree to restore")
        }
        nav.resumeAt(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW)
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