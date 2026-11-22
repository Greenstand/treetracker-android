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
package org.greenstand.android.TreeTracker.models.setupflow

import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.context.GlobalContext
import org.koin.core.scope.Scope
import timber.log.Timber

object CaptureSetupScopeManager {
    private var currentScope: Scope? = null

    val isOpen: Boolean get() = currentScope != null

    fun open() {
        if (currentScope != null) {
            Timber.tag("CaptureSetupScope").w("Scope already open, closing previous before reopening")
            close()
        }
        currentScope = CaptureSetupScope().scope
        // A new setup starts empty; saving it marks a setup as in progress (see restoreScope).
        currentScope?.get<CaptureSetupData>()?.save()
    }

    fun getData(): CaptureSetupData =
        (currentScope ?: restoreScope())?.get()
            ?: error("CaptureSetupScope not open. Call open() before getData().")

    val nav: CaptureSetupNavigationController
        get() =
            (currentScope ?: restoreScope())?.get()
                ?: error("CaptureSetupScope not open. Call open() before accessing nav.")

    fun close() {
        currentScope?.close()
        currentScope = null
        store()?.clear()
    }

    /**
     * Reopens the scope of a setup that was in progress when Android killed the process.
     *
     * Navigation 3 restores the setup screen the user was on (e.g. Add org) without the user
     * select screen under it, so nothing calls [open] before that screen asks for the scope
     * (#1418). The choices made so far come back from [CaptureSetupStore]; the navigation
     * controller finds its step from the back stack on the next `navForward`.
     *
     * Returns null when no setup was in progress ([open] was never called, or [close] ended it).
     */
    private fun restoreScope(): Scope? {
        val saved = store()?.load() ?: return null
        Timber.tag("CaptureSetupScope").i("Restoring the capture setup that was in progress")
        val scope = CaptureSetupScope().scope
        scope.get<CaptureSetupData>().restore(saved)
        currentScope = scope
        return scope
    }

    private fun store(): CaptureSetupStore? = GlobalContext.getOrNull()?.getOrNull<CaptureSetupStore>()
}

class CaptureSetupScope : KoinScopeComponent {
    override val scope: Scope by lazy {
        createScope(this)
    }
}
