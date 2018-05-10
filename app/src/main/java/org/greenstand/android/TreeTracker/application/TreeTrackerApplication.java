package org.greenstand.android.TreeTracker.application;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;

import org.greenstand.android.TreeTracker.BuildConfig;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class TreeTrackerApplication extends Application {
	  @Override
	  public void onCreate() {
	    // The following line triggers the initialization of ACRA
	    super.onCreate();
	    if(!BuildConfig.DEBUG) {
			Fabric.with(this, new Crashlytics());
		}

	    if (BuildConfig.DEBUG) {
			  Timber.plant(new Timber.DebugTree());
	    }
	  }

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);

	}
}
