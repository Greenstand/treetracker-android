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
