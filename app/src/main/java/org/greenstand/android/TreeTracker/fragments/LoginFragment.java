package org.greenstand.android.TreeTracker.fragments;

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

import org.apache.http.HttpStatus;
import org.greenstand.android.TreeTracker.BuildConfig;
import org.greenstand.android.TreeTracker.activities.MainActivity;
import org.greenstand.android.TreeTracker.R;
import org.greenstand.android.TreeTracker.api.Api;
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest;
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse;
import org.greenstand.android.TreeTracker.utilities.ValueHelper;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class LoginFragment extends Fragment implements OnClickListener {

    private static final String TAG = "LoginFragment";

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
      	      "org.greenstand.android", Context.MODE_PRIVATE);
	    
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

					AuthenticationRequest authRequest = new AuthenticationRequest();
					authRequest.setClientId(txtEmail);
					authRequest.setClientSecret(txtPass);

					MainActivity.progressDialog = new ProgressDialog(getActivity());
					MainActivity.progressDialog.setCancelable(false);
					MainActivity.progressDialog.setMessage(getActivity().getString(R.string.log_in_in_progress));
					MainActivity.progressDialog.show();

                    Call<TokenResponse> signIn = Api.instance().getApi().signIn(authRequest);
					signIn.enqueue(new Callback<TokenResponse>() {
                        @Override
                        public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                            MainActivity.progressDialog.dismiss();

                            if (response.isSuccessful()) {
                                SharedPreferences mSharedPreferences = getActivity().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE);
                                mSharedPreferences.edit().putString(ValueHelper.TOKEN, response.body().getToken()).commit();
                                Api.instance().setAuthToken(response.body().getToken());
                                ((MainActivity) getActivity()).transitionToMapsFragment();
                            } else {
								switch (response.code()) {
									case HttpStatus.SC_UNAUTHORIZED:
										Toast.makeText(getActivity(), "Incorrect username or password.", Toast.LENGTH_SHORT).show();
										break;

									case -1:
										Toast.makeText(getActivity(), "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show();
										break;

									default:
										break;
								}
							}
                        }

                        @Override
                        public void onFailure(Call<TokenResponse> call, Throwable t) {
                            MainActivity.progressDialog.dismiss();




							Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_SHORT).show();
                            Timber.tag(TAG).e(t.getMessage());
                        }
                    });
				}
				
				
				break;

			case R.id.fragment_login_login_do_not_have_account:
			
				mSharedPreferences = getActivity().getSharedPreferences(
		        	      "org.greenstand.android", Context.MODE_PRIVATE);
				
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



}
