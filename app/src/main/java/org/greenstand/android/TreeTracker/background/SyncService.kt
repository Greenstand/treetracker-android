package org.greenstand.android.TreeTracker.background

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel


class SyncService : JobIntentService() {

    private val jobScope = CoroutineScope(Dispatchers.IO)

    override fun onHandleWork(intent: Intent) {
        sendBroadcastMessage("")
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