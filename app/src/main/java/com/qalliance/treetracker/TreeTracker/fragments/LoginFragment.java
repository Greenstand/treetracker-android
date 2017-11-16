package com.qalliance.treetracker.TreeTracker.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.network.NetworkUtilities;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.Utils;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class LoginFragment extends Fragment implements OnClickListener {
	
	protected SharedPreferences mSharedPreferences;


	public LoginFragment() {
		//some overrides and settings go here
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
	
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.clear();
	}
    	 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
	    View v = inflater.inflate(R.layout.fragment_login, container, false);
	    
	    
	    ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
	    
	    
	    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	    
	    Button loginBtn = (Button) v.findViewById(R.id.fragment_login_login);
	    loginBtn.setOnClickListener(LoginFragment.this);
	    
	    TextView loginText = (TextView) v.findViewById(R.id.fragment_login_login_do_not_have_account);
	    loginText.setPaintFlags(loginText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	    
	    loginText.setOnClickListener(LoginFragment.this);
	    
	    TextView forgotPasswordText = (TextView) v.findViewById(R.id.fragment_login_login_forgot_password);
	    forgotPasswordText.setPaintFlags(forgotPasswordText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	    
	    forgotPasswordText.setOnClickListener(LoginFragment.this);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(
      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);
	    
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_login_login:
				

				String txtEmail = "";
				String txtPass = "";
				
				boolean validForm = true;

				EditText loginEmail = (EditText) getActivity().findViewById(R.id.fragment_login_email_address);
				EditText loginPassword = (EditText) getActivity().findViewById(R.id.fragment_login_password);

				
				if (loginPassword.getText().length() == 0) {
					loginPassword.setError("Please enter your password.");
					loginPassword.requestFocus();
					validForm = false;
				} else {
					txtPass = loginPassword.getText().toString();
				}

				if (loginEmail.getText().length() == 0) {
					loginEmail.setError("Please enter your e-mail address.");
					loginEmail.requestFocus();
					validForm = false;
				} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail.getText()).matches()) {
					loginEmail.setError("Please enter valid e-mail address.");
					loginEmail.requestFocus();
					validForm = false;
				} else {
					txtEmail = loginEmail.getText().toString();
					mSharedPreferences.edit().putString(ValueHelper.USERNAME, txtEmail).commit();
				}

				

				
				if (validForm) {
					InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(loginEmail.getWindowToken(), 0);
					
					JSONObject loginObj = new JSONObject();
					try {
						loginObj.put("username", txtEmail);
						loginObj.put("password", txtPass);
						
						NetworkUtilities.attemptLogin(loginObj, MainActivity.mHandler, getActivity());
						
						MainActivity.progressDialog = new ProgressDialog(getActivity());
						MainActivity.progressDialog.setCancelable(false);
						MainActivity.progressDialog.setMessage(getActivity().getString(R.string.log_in_in_progress));
						MainActivity.progressDialog.show();

						
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				
				break;

			case R.id.fragment_login_login_do_not_have_account:
			
				mSharedPreferences = getActivity().getSharedPreferences(
		        	      "com.qalliance.treetracker", Context.MODE_PRIVATE);
				
				mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, true).commit();
				mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();
				
				SignupFragment fragment = new SignupFragment();
				Bundle bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);
				
				FragmentTransaction fragmentTransaction1 = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction1.replace(R.id.container_fragment, fragment).commit();
				
				break;
			case R.id.fragment_login_login_forgot_password:
				
				ForgotPasswordFragment fragment2 = new ForgotPasswordFragment();
				Bundle bundle2 = getActivity().getIntent().getExtras();
				fragment2.setArguments(bundle2);
				
				FragmentTransaction fragmentTransaction2 = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction2.replace(R.id.container_fragment, fragment2).addToBackStack(ValueHelper.FORGOT_PASSWORD_FRAGMENT).commit();
				
				break;
		}
		
	}

	
    protected void sendJson(final String email, final String pwd) {
        Thread t = new Thread() {

            private Fragment fragment;
			private Bundle bundle;
			private FragmentTransaction fragmentTransaction;

			public void run() {
                Looper.prepare(); //For Preparing Message Pool for the child Thread
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;
                JSONObject json = new JSONObject();

                try {
                    HttpPost post = new HttpPost("http://kresimirplese.apiary.io/login");
                    json.put("email", email);
                    json.put("password", pwd);
                    StringEntity se = new StringEntity( json.toString());  
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    post.setEntity(se);
                    post.setHeader("Accept-Charset","utf-8");
                    response = client.execute(post);

                    /*Checking response */
                    if(response!=null){
                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
                        
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            String rsp = Utils.convertStreamToString(in);
                            
                            Toast.makeText(getActivity(), rsp, Toast.LENGTH_SHORT).show();
                            
                            //TODO add some logic about user login
                            //Below code unsets the login and signup fragments because user successfully loged in 
                            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
                            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();
                            
                            //Redirect to HomeFragment
//                            fragment = new HomeFragment();
							fragment = new MapsFragment();
            				bundle = getActivity().getIntent().getExtras();
            				fragment.setArguments(bundle);
            				
            				fragmentTransaction = getActivity().getSupportFragmentManager()
            						.beginTransaction();
            				fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.MAP_FRAGMENT).commit();

                        } else {
                        	String rsp = Utils.convertStreamToString(in);
//                        	Toast.makeText(getActivity(), "neki drugi status" + rsp, Toast.LENGTH_LONG).show();
                        }
                        
                        
                        
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                    //createDialog("Error", "Cannot Estabilish Connection");
                }

                Looper.loop(); //Loop in the message queue
            }
        };

        t.start();      
    }

}
