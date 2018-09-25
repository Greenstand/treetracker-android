package org.greenstand.android.TreeTracker.network

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import android.widget.Toast

import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.DbHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.DatabaseManager
import timber.log.Timber

import java.util.Calendar

class NetworkReceiver : BroadcastReceiver() {


    private var mSharedPreferences: SharedPreferences? = null

    override fun onReceive(context: Context, intent: Intent) {

        mSharedPreferences = context.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        if (mSharedPreferences!!.getBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false) || mSharedPreferences!!.getBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false)) {

            return
        }


        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting

        var isWiFi = false
        if (activeNetwork != null) {
            isWiFi = activeNetwork.type == ConnectivityManager.TYPE_WIFI
        }

        val c = Calendar.getInstance()
        val timeOfDay = c.get(Calendar.HOUR_OF_DAY)   // to pop up  notication if anything is there to sync at 6am



        if (isConnected && isWiFi || timeOfDay == 6) {
            Toast.makeText(context, "WIFI CONNECTED", Toast.LENGTH_LONG).show()

            val treeCursor = TreeTrackerApplication.getDatabaseManager().queryCursor("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null)
            //this too
            treeCursor.moveToFirst()   //this
            val tosync = Integer.parseInt(treeCursor.getString(treeCursor.getColumnIndex("tosync")))
            Timber.d("to sync " + treeCursor.getString(treeCursor.getColumnIndex("tosync")))


            var notification: Uri? = null
            try {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } catch (e: Exception) {
            }

            val mBuilder = NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(context.resources.getString(R.string.treetracker))
                    .setTicker(context.resources.getString(R.string.wifi_connected))
                    .setSound(notification)
                    .setContentText(context.resources.getString(R.string.touch_to_sync))

            if (tosync != 0) {                                                    //if to sync has any data to sync
                // Creates an explicit intent for an Activity in your app
                val resultIntent = Intent(context, MainActivity::class.java)
                resultIntent.putExtra(ValueHelper.RUN_FROM_NOTIFICATION_SYNC, true)

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                val stackBuilder = TaskStackBuilder.create(context)
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity::class.java)
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent)
                val resultPendingIntent = stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
                mBuilder.setContentIntent(resultPendingIntent)
                val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // mId allows you to update the notification later on.

                mNotificationManager.notify(ValueHelper.WIFI_NOTIFICATION_ID, mBuilder.build())
            }
        } else {
            Toast.makeText(context, "WIFI DISCONNECTED", Toast.LENGTH_LONG).show()
        }

    }

}
