package org.greenstand.android.TreeTracker.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import org.greenstand.android.TreeTracker.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.greenstand.android.TreeTracker.api.Api;
import org.greenstand.android.TreeTracker.api.models.requests.ForgotPasswordRequest;
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
			
			if (forgotEmail.getText().length() == 0) {
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

				ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
				forgotPasswordRequest.setClientId(forgotEmail.getText().toString());
				Call<Void> forgotPassword = Api.instance().getApi().passwordReset(forgotPasswordRequest);
				forgotPassword.enqueue(new Callback<Void>() {
					@Override
					public void onResponse(Call<Void> call, Response<Void> response) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

						builder.setTitle(R.string.password_reset);

						final boolean mailOK = true; // Need to check what the server returned
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
					}

					@Override
					public void onFailure(Call<Void> call, Throwable t) {

					}
				});
			}
				
				
			break;

		}
		
	}


}
