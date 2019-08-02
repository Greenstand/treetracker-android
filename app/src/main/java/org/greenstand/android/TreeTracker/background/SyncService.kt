package org.greenstand.android.TreeTracker.background

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.viewmodels.SyncDataUseCase
import org.koin.android.ext.android.inject

class SyncService : JobIntentService() {

    private val jobScope = CoroutineScope(Dispatchers.IO)

    private val syncDataUseCase: SyncDataUseCase by inject()

    override fun onCreate() {
        super.onCreate()

//        startKoin {
//            androidLogger()
//            androidContext(applicationContext)
//            modules(
//                appModule,
//                networkModule,
//                roomModule
//            )
//        }
    }

    override fun onHandleWork(intent: Intent) {
        jobScope.launch {
            syncDataUseCase.execute(Unit)
        }
    }

    @ExperimentalCoroutinesApi
    override fun onStopCurrentWork(): Boolean {
        jobScope.cancel()
        return false
    }

    companion object {

        private const val JOB_ID = 928

        fun enqueueWork(context: Context) {
            enqueueWork(context, SyncService::class.java, JOB_ID, Intent(context, SyncService::class.java))
        }

    }
}