package com.qalliance.treetracker.TreeTracker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

public class NetworkReceiver extends BroadcastReceiver{


	private SharedPreferences mSharedPreferences;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		mSharedPreferences = context.getSharedPreferences(
	      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);
		
		if ((mSharedPreferences.getBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false) ||
		        mSharedPreferences.getBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false))) {
			
			return;
		}
		
		
		
		ConnectivityManager cm =
		        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnectedOrConnecting();
		
		boolean isWiFi = false;
		if (activeNetwork != null) {
			isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
		}
		
		
		
		if (isConnected && isWiFi) {
			Toast.makeText(context, "WIFI CONNECTED", Toast.LENGTH_LONG).show();
			
			Uri notification = null;
			try {
		         notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		    } catch (Exception e) {}
			
			NotificationCompat.Builder mBuilder =
			        new NotificationCompat.Builder(context)
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setContentTitle(context.getResources().getString(R.string.treetracker))
			        .setTicker(context.getResources().getString(R.string.wifi_connected))
			        .setSound(notification)
			        .setContentText(context.getResources().getString(R.string.touch_to_sync));
			// Creates an explicit intent for an Activity in your app
			Intent resultIntent = new Intent(context, MainActivity.class);
			resultIntent.putExtra(ValueHelper.RUN_FROM_NOTIFICATION_SYNC, true);

			// The stack builder object will contain an artificial back stack for the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out of
			// your application to the Home screen.
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(MainActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
			        stackBuilder.getPendingIntent(
			            0,
			            PendingIntent.FLAG_UPDATE_CURRENT
			        );
			mBuilder.setContentIntent(resultPendingIntent);
			NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(ValueHelper.WIFI_NOTIFICATION_ID, mBuilder.build());
			
		} else {
			Toast.makeText(context, "WIFI DISCONNECTED", Toast.LENGTH_LONG).show();
		}
		
	}

}
