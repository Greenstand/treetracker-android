/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.qalliance.treetracker.TreeTracker.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.qalliance.treetracker.TreeTracker.BuildConfig;
import com.qalliance.treetracker.TreeTracker.utilities.Utils;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;

/**
 * Provides utility methods for communicating with the server.
 */
public class NetworkUtilities {
    private static final String TAG = "NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String BASE_URL = BuildConfig.BASE_URL;
    public static final String FORGOT_PASSWORD_URL = BASE_URL + "/password/remind";
    public static final String AUTH_URI = BASE_URL + "/users/login";
    public static final String SIGNUP_URI = BASE_URL + "/users/signup";
    public static final String TREE_SYNC_URI = BASE_URL + "/trees/";
    public static final String TREE_FOR_USER_URI = BASE_URL + "/trees/user/";
    public static final String SETTINGS_SYNC_URI = BASE_URL + "/settings/";
    public static final String PENDING_UPDATES_URI = BASE_URL + "/trees/updates/user/";
    public static final String PENDING_UPDATES_CLEAR_URI = BASE_URL + "/trees/updates/";
    
    
    public static final boolean mAlreadySending = true;

    private static HttpClient mHttpClient;

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static void maybeCreateHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = createHttpClient();
        }
    }

    /**
     * Constructs a new DefaultHttpClient with a default 30 second timeout.
     */
    public static DefaultHttpClient createHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params,
            REGISTRATION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
        ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        return client;
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };

        t.start();
        return t;
    }

    /**
     * Connects to the  server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean signup(JSONObject signupObj,
        Handler handler, final Context context) {
        final HttpResponse resp;

       
        StringEntity se = null;
		try {
			se = new StringEntity(signupObj.toString());
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  
  
		Log.e("json string", signupObj.toString());
		  
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		Log.e("SIGNUP_URI", SIGNUP_URI);
        
        final HttpPost post = new HttpPost(SIGNUP_URI);
        post.setEntity(se);
        post.setHeader("Accept-Charset","utf-8");
        maybeCreateHttpClient();
        
        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Log.v(TAG, "Successful authentication");
                    
                InputStream in = resp.getEntity().getContent(); //Get the data in the entity

                String rsp = Utils.convertStreamToString(in);
                    
                sendSignupResult(true, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return true;
            } else {
            	InputStream in = resp.getEntity().getContent(); //Get the data in the entity
                Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                String rsp = Utils.convertStreamToString(in);
                sendSignupResult(false, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return false;
            }
        } catch (final ConnectException e) {
            Log.d(TAG, "Connect exception", e);
            
           
            sendSignupResult(false, handler, context, -1000, null);
        return false;
        
        } catch (final IOException e) {
                Log.v(TAG, "IOException when getting authtoken", e);
            sendSignupResult(false, handler, context, -1, null);
            return false;
        } finally {
                Log.v(TAG, "getAuthtoken completing");
        }
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding signup result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     * @param httpResponseCode 
     * @param responseBody 
     */
    private static void sendSignupResult(final Boolean result, final Handler handler,
        final Context context, final int httpResponseCode, final String responseBody) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((MainActivity) context).onSignupResult(result, httpResponseCode, responseBody);
            }
        });
    }
    
    
    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding signup result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     * @param httpResponseCode 
     * @param responseBody 
     */
    private static void sendLoginResult(final Boolean result, final Handler handler,
        final Context context, final int httpResponseCode, final String responseBody) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((MainActivity) context).onLoginResult(result, httpResponseCode, responseBody);
            }
        });
    }
    
    
    /**
     * Sends the tree sync response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding sync result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     * @param httpResponseCode 
     * @param responseBody 
     */
    private static void sendTreeSyncResult(final Boolean result, final Handler handler,
        final Context context, final int httpResponseCode, final String responseBody) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((MainActivity) context).onTreeSyncResult(result, httpResponseCode, responseBody);
            }
        });
    }

    /**
     * Attempts to signup the user on the server.
     * 
     * @param signupObj is a json object containing all the data
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptSignup(final JSONObject signupObj, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                signup(signupObj, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }

    
    /**
     * Attempts to signup the user on the server.
     * 
     * @param signupObj is a json object containing all the data
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptLogin(final JSONObject signupObj, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                login(signupObj, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }
    
    
    /**
     * Attempts to sync the tree on the server.
     * 
     * @param signupObj is a json object containing all the data
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptTreeSync(final JSONObject signupObj, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                treeSync(signupObj, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }

    /**
     * Connects to the  server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean login(JSONObject loginObj,
        Handler handler, final Context context) {
        final HttpResponse resp;

       
        StringEntity se = null;
		try {
			se = new StringEntity(loginObj.toString());
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  
  
		Log.e("json login string", loginObj.toString());
		  
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		Log.e("AUTH_URI", AUTH_URI);
        
        final HttpPost post = new HttpPost(AUTH_URI);
        post.setEntity(se);
        post.setHeader("Accept-Charset","utf-8");
        maybeCreateHttpClient();
        
        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Log.v(TAG, "Successful authentication");
                    
                InputStream in = resp.getEntity().getContent(); //Get the data in the entity

                String rsp = Utils.convertStreamToString(in);
                    
                sendLoginResult(true, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return true;
            } else {
            	InputStream in = resp.getEntity().getContent(); //Get the data in the entity
                Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                String rsp = Utils.convertStreamToString(in);
                sendLoginResult(false, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return false;
            }
        } catch (final ConnectException e) {
            Log.d(TAG, "Connect exception", e);
            
           
            sendLoginResult(false, handler, context, -1000, null);
        return false;
        
        } catch (final IOException e) {
                Log.v(TAG, "IOException when getting authtoken", e);
                sendLoginResult(false, handler, context, -1, null);
            return false;
        } finally {
                Log.v(TAG, "getAuthtoken completing");
        }
    }
    
    
    /**
     * Connects to the  server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean treeSync(JSONObject loginObj,
        Handler handler, final Context context) {
        final HttpResponse resp;

       
        StringEntity se = null;
		try {
			se = new StringEntity(loginObj.toString());
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  
  
		Log.e("json sync string", loginObj.toString());
		
		  
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		
        
        HttpPut post = null;
		try {
			Log.e("TREE_SYNC_URI", TREE_SYNC_URI + loginObj.getString("id"));
			post = new HttpPut(TREE_SYNC_URI + loginObj.getString("id"));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        post.setEntity(se);
        post.setHeader("Accept-Charset","utf-8");
        maybeCreateHttpClient();
        
        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Log.v(TAG, "Successful authentication");
                    
                InputStream in = resp.getEntity().getContent(); //Get the data in the entity

                String rsp = Utils.convertStreamToString(in);
                    
                sendTreeSyncResult(true, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return true;
            } else {
            	InputStream in = resp.getEntity().getContent(); //Get the data in the entity
                Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                String rsp = Utils.convertStreamToString(in);
                sendTreeSyncResult(false, handler, context, resp.getStatusLine().getStatusCode(), rsp);
                return false;
            }
        } catch (final ConnectException e) {
            Log.d(TAG, "Connect exception", e);
            
           
            sendTreeSyncResult(false, handler, context, -1000, null);
        return false;
        
        } catch (final IOException e) {
                Log.v(TAG, "IOException when getting authtoken", e);
                sendTreeSyncResult(false, handler, context, -1, null);
            return false;
        } finally {
                Log.v(TAG, "getAuthtoken completing");
        }
    }


}
