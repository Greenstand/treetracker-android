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