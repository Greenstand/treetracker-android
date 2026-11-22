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

import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope
import timber.log.Timber
import java.io.Serializable

/**
 * The contents of [CaptureSetupData] in a form that fits in saved instance state. The user is
 * kept by id and read from the database again on restore.
 */
data class CaptureSetupSnapshot(
    val userId: Long? = null,
    val destinationWallet: String? = null,
    val sessionNote: String? = null,
    val organizationName: String? = null,
) : Serializable

object CaptureSetupScopeManager {
    private var currentScope: Scope? = null

    val isOpen: Boolean get() = currentScope != null

    fun open() {
        if (currentScope != null) {
            Timber.tag("CaptureSetupScope").w("Scope already open, closing previous before reopening")
            close()
        }
        currentScope = CaptureSetupScope().scope
        currentScope?.get<CaptureSetupData>()
    }

    fun getData(): CaptureSetupData =
        currentScope?.get()
            ?: error("CaptureSetupScope not open. Call open() before getData().")

    val nav: CaptureSetupNavigationController
        get() =
            currentScope?.get()
                ?: error("CaptureSetupScope not open. Call open() before accessing nav.")

    fun close() {
        currentScope?.close()
        currentScope = null
    }

    /** The open scope's data, to save with the activity's state. Null while the scope is closed. */
    fun snapshot(): CaptureSetupSnapshot? {
        val data = currentScope?.get<CaptureSetupData>() ?: return null
        return CaptureSetupSnapshot(
            userId = data.user?.id,
            destinationWallet = data.destinationWallet,
            sessionNote = data.sessionNote,
            organizationName = data.organizationName,
        )
    }

    /**
     * Reopens the scope from a [snapshot] saved before the process was killed.
     *
     * The scope lives only in memory, but Navigation 3 restores the back stack, so without this
     * the app comes back on a setup screen (e.g. Add org) whose ViewModel calls [getData] with
     * no scope open. Changes nothing while the scope is open: the process survived (e.g. a
     * rotation) and the data in memory is the newest.
     *
     * The user stays null when it is no longer in the database.
     */
    suspend fun restore(
        snapshot: CaptureSetupSnapshot,
        userRepo: UserRepo,
    ) {
        if (isOpen) return
        val savedUser = snapshot.userId?.let { userRepo.getUser(it) }
        open()
        getData().apply {
            user = savedUser
            destinationWallet = snapshot.destinationWallet
            sessionNote = snapshot.sessionNote
            organizationName = snapshot.organizationName
        }
    }
}

class CaptureSetupScope : KoinScopeComponent {
    override val scope: Scope by lazy {
        createScope(this)
    }
}