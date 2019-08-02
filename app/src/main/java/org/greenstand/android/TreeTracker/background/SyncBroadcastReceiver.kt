package org.greenstand.android.TreeTracker.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class SyncBroadcastReceiver : BroadcastReceiver() {

    var onDataReceived: () -> Unit = { }

    override fun onReceive(context: Context, intent: Intent) {
        onDataReceived()
    }

}
