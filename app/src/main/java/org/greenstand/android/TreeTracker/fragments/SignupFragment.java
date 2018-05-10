package org.greenstand.android.TreeTracker.fragments;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.greenstand.android.TreeTracker.BuildConfig;
import org.greenstand.android.TreeTracker.activities.MainActivity;
import org.greenstand.android.TreeTracker.R;
import org.greenstand.android.TreeTracker.api.Api;
import org.greenstand.android.TreeTracker.api.ApiService;
import org.greenstand.android.TreeTracker.api.models.requests.RegisterRequest;
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse;
import org.greenstand.android.TreeTracker.api.models.responses.UserTree;
import org.greenstand.android.TreeTracker.utilities.ValueHelper;

import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class SignupFragment extends Fragment implements OnClickListener {

    public static final String TAG = "SignupFragment";

	public SignupFragment() {
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

		View v = inflater.inflate(R.layout.fragment_signup, container, false);

		((AppCompatActivity)getActivity()).getSupportActionBar().hide();

		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		Button signUpBtn = (Button) v.findViewById(R.id.fragment_signup_signup);
		signUpBtn.setOnClickListener(SignupFragment.this);

		if (BuildConfig.DEBUG) {
			//It's not a release version.
			signUpBtn.setOnLongClickListener(new Button.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					EditText signupFirstName = (EditText) getActivity().findViewById(R.id.fragment_signup_first_name);
					EditText signupLastName = (EditText) getActivity().findViewById(R.id.fragment_signup_last_name);
					EditText signupEmail = (EditText) getActivity().findViewById(R.id.fragment_signup_email_address);
					EditText signupPassword = (EditText) getActivity().findViewById(R.id.fragment_signup_password);
					EditText signupOrganization = (EditText) getActivity().findViewById(R.id.fragment_signup_organization);
					EditText signupPhone = (EditText) getActivity().findViewById(R.id.fragment_signup_phone_number);

					signupFirstName.setText("First Name Test");
					signupLastName.setText("Last Name Test");
					signupEmail.setText(UUID.randomUUID().toString() + "@greenstand.org");
					signupPassword.setText("tttttttt");
					signupOrganization.setText("Greenstand");
					signupPhone.setText("1234567890");

					return true;
				}
			});
		}

		TextView loginText = (TextView) v.findViewById(R.id.fragment_signup_login_already_have_account);
		loginText.setText(Html.fromHtml(loginText.getText() + " <a style=\"color:#916B4A;\" href=\"http://www.google.com\">"
				+ getResources().getString(R.string.log_in) + ".</a> "));
		Linkify.addLinks(loginText, Linkify.ALL);
		//loginText.setMovementMethod(LinkMovementMethod.getInstance());

		loginText.setOnClickListener(SignupFragment.this);

		TextView fragment_signup_privacy_policy_link = (TextView) v.findViewById(R.id.fragment_signup_privacy_policy_link);
		fragment_signup_privacy_policy_link.setMovementMethod(LinkMovementMethod.getInstance());

		return v;
	}

	public void onClick(View v) {


		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
				"org.greenstand.android", Context.MODE_PRIVATE);


		switch (v.getId()) {
			case R.id.fragment_signup_signup:

				String txtFirstName = "";
				String txtLastName = "";
				String txtEmail = "";
				String txtPass = "";
				String txtOrg = "";
				String txtPhone = "";

				boolean validForm = true;

				EditText signupFirstName = (EditText) getActivity().findViewById(R.id.fragment_signup_first_name);
				EditText signupLastName = (EditText) getActivity().findViewById(R.id.fragment_signup_last_name);
				EditText signupEmail = (EditText) getActivity().findViewById(R.id.fragment_signup_email_address);
				EditText signupPassword = (EditText) getActivity().findViewById(R.id.fragment_signup_password);
				EditText signupOrganization = (EditText) getActivity().findViewById(R.id.fragment_signup_organization);
				EditText signupPhone = (EditText) getActivity().findViewById(R.id.fragment_signup_phone_number);

				txtOrg = signupOrganization.getText().toString();
				txtPhone = signupPhone.getText().toString();

				CheckBox signupPrivacyPolicy=(CheckBox) getActivity().findViewById(R.id.fragment_signup_privacy_policy_checkbox);

				if (signupPassword.getText().length() == 0) {
					signupPassword.setError("Please enter your password.");
					signupPassword.requestFocus();
					validForm = false;
				} else {
					txtPass = signupPassword.getText().toString();
				}

				if (signupEmail.getText().length() == 0) {
					signupEmail.setError("Please enter your e-mail address.");
					signupEmail.requestFocus();
					validForm = false;
				} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(signupEmail.getText()).matches()) {
					signupEmail.setError("Please enter valid e-mail address.");
					signupEmail.requestFocus();
					validForm = false;
				} else {
					txtEmail = signupEmail.getText().toString();
					sharedPreferences.edit().putString(ValueHelper.USERNAME, txtEmail).commit();
				}

				if (signupLastName.getText().length() == 0) {
					signupLastName.setError("Please enter your last name.");
					signupLastName.requestFocus();
					validForm = false;
				} else {
					txtLastName = signupLastName.getText().toString();
				}

				if (signupFirstName.getText().length() == 0) {
					signupFirstName.setError("Please enter your first name.");
					signupFirstName.requestFocus();
					validForm = false;
				} else {
					txtFirstName = signupFirstName.getText().toString();
				}

				if(!signupPrivacyPolicy.isChecked())
				{
					signupPrivacyPolicy.setError("Please accept the terms of service and privacy policy.");
					signupPrivacyPolicy.requestFocus();
					validForm = false;
				}

				if (validForm) {

					InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(signupEmail.getWindowToken(), 0);

					RegisterRequest registerRequest = new RegisterRequest();
					registerRequest.setFirstName(txtFirstName);
					registerRequest.setLastName(txtLastName);
					registerRequest.setClientId(txtEmail);
					registerRequest.setClientSecret(txtPass);
					registerRequest.setOrganization(txtOrg);
					registerRequest.setPhone(txtPhone);


                    MainActivity.progressDialog = new ProgressDialog(getActivity());
                    MainActivity.progressDialog.setCancelable(false);
                    MainActivity.progressDialog.setMessage(getActivity().getString(R.string.sign_up_in_progress));
                    MainActivity.progressDialog.show();

                    final String finalFirstName = txtFirstName;
                    final String finalLastName = txtLastName;

                    Call<TokenResponse> register = Api.instance().getApi().register(registerRequest);
                    register.enqueue(new Callback<TokenResponse>() {
                        @Override
                        public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                            MainActivity.progressDialog.cancel();

                            if (response.isSuccessful()) {

                                SharedPreferences mSharedPreferences = getActivity().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE);
                                mSharedPreferences.edit().putString(ValueHelper.TOKEN, response.body().getToken()).commit();
								Api.instance().setAuthToken(response.body().getToken());

                                ((MainActivity) getActivity()).transitionToMapsFragment();
                            }
                        }

                        @Override
                        public void onFailure(Call<TokenResponse> call, Throwable t) {
                            MainActivity.progressDialog.cancel();
                            Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_SHORT).show();
                            Timber.tag(TAG).e(t.getMessage());
                        }
                    });



				}


				break;
			case R.id.fragment_signup_login_already_have_account:

				sharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
				sharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, true).commit();


				LoginFragment fragment = new LoginFragment();
				Bundle bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);

				FragmentTransaction fragmentTransaction1 = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction1.replace(R.id.container_fragment, fragment).commit();


				break;
		}

	}




}
