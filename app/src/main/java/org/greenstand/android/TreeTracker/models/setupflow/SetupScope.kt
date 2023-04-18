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
import org.koin.core.scope.Scope

object CaptureSetupScopeManager {

    private var currentScope: Scope? = null

    fun open() {
        currentScope = CaptureSetupScope().scope
        currentScope?.get<CaptureSetupData>()
    }

    fun getData(): CaptureSetupData = currentScope!!.get()

    val nav: CaptureSetupNavigationController
        get() = currentScope!!.get()

    fun close() {
        currentScope?.close()
    }
}

class CaptureSetupScope : KoinScopeComponent {
    override val scope: Scope by lazy {
        createScope(this)
    }
}