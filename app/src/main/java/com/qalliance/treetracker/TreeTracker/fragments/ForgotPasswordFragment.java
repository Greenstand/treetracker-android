package com.qalliance.treetracker.TreeTracker.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.qalliance.treetracker.TreeTracker.network.NetworkUtilities;
import com.qalliance.treetracker.TreeTracker.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;

public class ForgotPasswordFragment extends Fragment implements OnClickListener {
	
	private AsyncTask<String, Void, String> mResetPassTask;

	public ForgotPasswordFragment() {
		//some overrides and settings go here
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
    	 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
	    View v = inflater.inflate(R.layout.fragment_forgot_password, container, false);
	    
	    ((ActionBarActivity)getActivity()).getSupportActionBar().hide();
	    
	    Button loginBtn = (Button) v.findViewById(R.id.fragment_forgot_password_submit);
	    loginBtn.setOnClickListener(ForgotPasswordFragment.this);
	    
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_forgot_password_submit:
				
			boolean validForm = true;
			TextView forgotEmail = (TextView) getActivity().findViewById(R.id.fragment_forgot_password_email_address);
			
			if (forgotEmail .getText().length() == 0) {
				forgotEmail.setError("Please enter your e-mail address.");
				forgotEmail.requestFocus();
				validForm  = false;
			} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotEmail.getText()).matches()) {
				forgotEmail.setError("Please enter valid e-mail address.");
				forgotEmail.requestFocus();
				validForm = false;
			}
			
			
			if (validForm) {
				InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(forgotEmail.getWindowToken(), 0);

				mResetPassTask = new ResetPasswordTask().execute(new String[]{forgotEmail.getText().toString()});
			}
				
				
			break;

		}
		
	}

	class ResetPasswordTask extends AsyncTask<String, Void, String> {
		
    	private ProgressDialog progressDialog;
    	private boolean mailOK = true;

		protected void onPreExecute() {
    		progressDialog = new ProgressDialog(getActivity());
			progressDialog.setCancelable(false);
			progressDialog.setMessage(getActivity().getString(R.string.sending_password_reset_request));
			progressDialog.show();

    	}


		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
			String rsp = null;
	        
	        HttpPost post = null;

	        JSONObject jsonOut = new JSONObject();
	        
	        try {
				jsonOut.put("username", params[0]);
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

//	        {"username":"kresimir.plese@gmail.com"}


	        StringEntity se = null;
			try {
				se = new StringEntity(jsonOut.toString());
				Log.i("json", jsonOut.toString());
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}  
	  
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	        
			post = new HttpPost(NetworkUtilities.FORGOT_PASSWORD_URL);
	        post.setEntity(se);
	        post.setHeader("Accept-Charset","utf-8");


	        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
	        
	        try {
	            resp = mHttpClient.execute(post);
	        } catch (final ConnectException e) {
	            Log.d("", "Connect exception", e);
	        } catch (final IOException e) {
	                Log.v("", "IOException when getting authtoken", e);
	        } finally {
	                Log.v("", "getAuthtoken completing");
	        }
	        
			try {
				
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
						rsp = "Token invalid";
						return rsp;
					
				} else {
					rsp = EntityUtils.toString(resp.getEntity());
				}
				
				
				
			} catch (IllegalStateException e) {
				Log.e("onpostexec", "IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("onpostexec", "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Exception", "Exception");
				e.printStackTrace();
			}
		
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        if (progressDialog != null) {
		        	progressDialog.dismiss();
		        }
		        
		        if (response != null) {
		        	JSONObject jsonResponse = null;
		        	
		        	try {
						jsonResponse = new JSONObject(response);
						
						try {
							if (jsonResponse.get("error") != null) {
								mailOK = false;
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Log.e("response", response);
						
					} catch (JSONException e) {
					}
		        	
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					
					builder.setTitle(R.string.password_reset);
					
					if (mailOK) {
						builder.setMessage(R.string.reset_password_link_was_sent);
					} else {
						builder.setMessage(R.string.email_was_not_found);
					}
					 
					
					
					 
					builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					 
					   public void onClick(DialogInterface dialog, int which) {
						   
						   if (mailOK) {
							   getActivity().getSupportFragmentManager().popBackStack();
						   }
						   
						   
					         
					       dialog.dismiss();
					   }
					 
					});

					 
					 
					AlertDialog alert = builder.create();
					alert.show();
		        } else {
	        		
		        }
		 }
		
		
	}

}
