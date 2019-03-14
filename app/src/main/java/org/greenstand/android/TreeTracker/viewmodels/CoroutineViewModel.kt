package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlin.coroutines.CoroutineContext

abstract class CoroutineViewModel : ViewModel(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = UI

    override fun onCleared() {
        job.cancel()
    }
}