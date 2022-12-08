package org.greenstand.android.TreeTracker.models.captureflowdata

import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope

object CaptureFlowScopeManager {

    private var currentScope: Scope? = null

    fun open() {
        currentScope = CaptureFlowScope().scope
        currentScope?.get<CaptureFlowScope>()
    }

    fun getData(): CaptureFlowScope = currentScope!!.get()

    val nav: CaptureFlowNavigationController
        get() = currentScope!!.get()

    fun close() {
        currentScope?.close()
    }
}

class CaptureFlowScope : KoinScopeComponent {
    override val scope: Scope by lazy {
        createScope(this)
    }
}