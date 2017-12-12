package org.greenstand.android.TreeTracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import org.greenstand.android.TreeTracker.R;
import org.greenstand.android.TreeTracker.utilities.ValueHelper;

public class SplashActivity extends Activity{
	
	Thread mSplashThread;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_splash);
		

	        
	        // The thread to wait for splash screen events
	        mSplashThread = new Thread(){
	            @Override
	            public void run(){
	                try {
	                    synchronized(this){
	                        // Wait given period of time or exit on touch
	                        wait(ValueHelper.SPLASH_SCREEN_DURATION);
	                    }
	                }
	                catch(InterruptedException ex){                    
	                }
	                
	                // Run next activity
	                Intent intent = new Intent();
	                intent.setClass(SplashActivity.this, MainActivity.class);
	                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	                startActivity(intent);
	                overridePendingTransition(0, 0);
	                
	                finish();
	                overridePendingTransition(0, 0);
                 
	                
	            }
	        };
	        
	        mSplashThread.start();
	}
	
	/**
     * Processes splash screen touch events
     */
    @Override
    public boolean onTouchEvent(MotionEvent evt)
    {
        if(evt.getAction() == MotionEvent.ACTION_DOWN)
        {
            synchronized(mSplashThread){
                mSplashThread.notifyAll();
            }
        }
        return true;
    }   

}
