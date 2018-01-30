package org.greenstand.android.TreeTracker.application;

import android.app.Application;

//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;
import org.greenstand.android.TreeTracker.BuildConfig;

import timber.log.Timber;

public class TreeTrackerApplication extends Application {
	  @Override
	  public void onCreate() {
	    // The following line triggers the initialization of ACRA
	    super.onCreate();
	    //Fabric.with(this, new Crashlytics());

	    if (BuildConfig.DEBUG) {
			  Timber.plant(new Timber.DebugTree());
		  }
	  }

}
