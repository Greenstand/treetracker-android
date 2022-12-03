package org.greenstand.android.TreeTracker.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

suspend fun <Data> List<Data>.runInParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    action: suspend (Data) -> Unit
) = withContext(dispatcher) {
    this@runInParallel.map {
        async { action(it) }
    }.forEach {
        it.await()
    }
}