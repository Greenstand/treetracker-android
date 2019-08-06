package org.greenstand.android.TreeTracker.background

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.koin.android.ext.android.inject


class SyncService : JobIntentService() {

    private val jobScope = CoroutineScope(Dispatchers.IO)

    private val syncDataUseCase: SyncDataUseCase by inject()

    override fun onHandleWork(intent: Intent) {

        sendBroadcastMessage("")

        jobScope.launch {
//            syncDataUseCase.execute {
//                sendBroadcastMessage("")
//            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onStopCurrentWork(): Boolean {
        jobScope.cancel()
        return false
    }

    fun sendBroadcastMessage(message: String) {
        val localIntent = Intent(ACTION_ID)
        localIntent.putExtra("result", message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    companion object {

        const val ACTION_ID = "org.greenstand.android.TreeTracker.background.SyncService"

        private const val JOB_ID = 928

        fun enqueueWork(context: Context) {
            enqueueWork(context, SyncService::class.java, JOB_ID, Intent(context, SyncService::class.java))
        }

    }
}