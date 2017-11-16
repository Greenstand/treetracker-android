package com.qalliance.treetracker.TreeTracker.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

public class SettingsFragment extends Fragment implements OnClickListener, OnCheckedChangeListener, 
														  TextWatcher {
	
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;
	private SharedPreferences mSharedPreferences;

	public SettingsFragment() {
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
		
	    View v = inflater.inflate(R.layout.fragment_settings, container, false);
	    
	    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(
	      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);
	    
	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.settings);
//		((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	    RadioGroup whichSettings = (RadioGroup) v.findViewById(R.id.fragment_settings_which_settings);
	    whichSettings.setOnCheckedChangeListener(SettingsFragment.this);
	    
	    RadioGroup manualRadioSettings = (RadioGroup) v.findViewById(R.id.fragment_settings_manual_settings_radio_group);
	    manualRadioSettings.setOnCheckedChangeListener(SettingsFragment.this);
	    
	    CheckBox saveAndEdit = (CheckBox) v.findViewById(R.id.fragment_settings_save_and_edit);
    	saveAndEdit.setChecked(mSharedPreferences.getBoolean(ValueHelper.SAVE_AND_EDIT, false));
	    
	    EditText nextUpdate = (EditText) v.findViewById(R.id.fragment_settings_next_update);
	    nextUpdate.addTextChangedListener(SettingsFragment.this);
	    
	    nextUpdate.setText(Integer.toString(mSharedPreferences.getInt(
				ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences.getInt(
						ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
						ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING))));
	    
    	TextView gpsAcc = (TextView) v.findViewById(R.id.fragment_settings_gps_accuracy);
	    if (mSharedPreferences.getBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)) {
	    	whichSettings.check(R.id.fragment_settings_treetracker);
	    	
	    	
			int accServer = mSharedPreferences.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);
			int nextUpdateServer = mSharedPreferences.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING);
			
			mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, accServer).commit();
			mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, nextUpdateServer).commit();
	    	
	    	//TODO replace with data from server
	    	nextUpdate.setText(Integer.toString(nextUpdateServer));
	    	
	    	gpsAcc.setText(Integer.toString(accServer)  + " " + getActivity().getResources().getString(R.string.meters));
	    	
	    	nextUpdate.setEnabled(false);
	    	Log.i("radio group", "treetracker");
	    } else {
	    	LinearLayout manualSettings = (LinearLayout) v.findViewById(R.id.fragment_settings_manual_settings);
	    	Log.i("radio group", "manual");
	    	whichSettings.check(R.id.fragment_settings_manual);
	    	manualSettings.setVisibility(View.VISIBLE);
	    	nextUpdate.setEnabled(true);
	    	
	    	gpsAcc.setVisibility(View.INVISIBLE);
	    	saveAndEdit.setVisibility(View.VISIBLE);
	    }
	    
	    
	    for (int i = 0; i < manualRadioSettings.getChildCount(); i++) {
			RadioButton rb = (RadioButton) manualRadioSettings.getChildAt(i);
			
			if (rb != null) {
				
				
				if (Integer.parseInt(rb.getText().toString()) == 
						mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING) ) {
					Log.i("rb text", rb.getText().toString());
					manualRadioSettings.check(rb.getId());
					break;
				}
			}
		}
	    
	    Button submitBtn = (Button) v.findViewById(R.id.fragment_settings_submit);
	    submitBtn.setOnClickListener(SettingsFragment.this);
	    
	    for (int i = 0; i < manualRadioSettings.getChildCount(); i++) {
	    	((RadioButton)manualRadioSettings.getChildAt(i)).setText(((RadioButton)manualRadioSettings.getChildAt(i)).getText() 
	    			+ " "
	    			+ getActivity().getResources().getString(R.string.meters));
		}

	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_settings_submit:
				
				RadioGroup radioGroup = (RadioGroup) getActivity().findViewById(R.id.fragment_settings_which_settings);
			    CheckBox saveAndEdit = (CheckBox) getActivity().findViewById(R.id.fragment_settings_save_and_edit);
			    
				if (radioGroup.getCheckedRadioButtonId() == R.id.fragment_settings_treetracker) {
					mSharedPreferences.edit().putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true).commit();
					
					int acc = mSharedPreferences.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);
					int nextUpdate = mSharedPreferences.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING);
					
					mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, acc).commit();
					mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, nextUpdate).commit();
					
				} else {
					mSharedPreferences.edit().putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, false).commit();
					

					EditText nextUpdate = (EditText) getActivity().findViewById(R.id.fragment_settings_next_update);
					
					mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, Integer.parseInt(nextUpdate.getText().toString())).commit();
					
					RadioGroup accuracyRadioGroup = (RadioGroup) getActivity().findViewById(R.id.fragment_settings_manual_settings_radio_group);
					RadioButton selectedRadioButton = (RadioButton) getActivity().findViewById(accuracyRadioGroup.getCheckedRadioButtonId());
					
					if (selectedRadioButton != null) {
						mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 
								Integer.parseInt(selectedRadioButton.getText().toString().substring(0, selectedRadioButton.getText().toString().lastIndexOf(" ")))).commit();
					}
					
					if (saveAndEdit.isChecked()) {
						mSharedPreferences.edit().putBoolean(ValueHelper.SAVE_AND_EDIT, true).commit();
					} else {
						mSharedPreferences.edit().putBoolean(ValueHelper.SAVE_AND_EDIT, false).commit();
					}
					
					
				}
				
				Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
				getActivity().getSupportFragmentManager().popBackStack();
				break;
		}
	}

	public void onCheckedChanged(RadioGroup radiogroup, int checkedId) {
		LinearLayout manualSettings = (LinearLayout) getActivity().findViewById(R.id.fragment_settings_manual_settings);
		EditText nextUpdate = (EditText) getActivity().findViewById(R.id.fragment_settings_next_update);
		TextView gpsAcc = (TextView) getActivity().findViewById(R.id.fragment_settings_gps_accuracy);
		CheckBox saveAndEdit = (CheckBox) getActivity().findViewById(R.id.fragment_settings_save_and_edit);
		
		
		if (manualSettings == null) {
			return;
		}
		
		switch (checkedId) {
		case R.id.fragment_settings_treetracker:
			manualSettings.setVisibility(View.GONE);
			saveAndEdit.setVisibility(View.GONE);
			nextUpdate.setEnabled(false);
			gpsAcc.setVisibility(View.VISIBLE);
			
			int accServer = mSharedPreferences.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);
			int nextUpdateServer = mSharedPreferences.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING);
			
			nextUpdate.setText(Integer.toString(accServer));
			gpsAcc.setText(Integer.toString(nextUpdateServer) + " " + getActivity().getResources().getString(R.string.meters));
			
			break;

		case R.id.fragment_settings_manual:
			manualSettings.setVisibility(View.VISIBLE);
			saveAndEdit.setVisibility(View.VISIBLE);
			nextUpdate.setEnabled(true);
			gpsAcc.setVisibility(View.INVISIBLE);
			
			nextUpdate.setText(Integer.toString(mSharedPreferences.getInt(
					ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences.getInt(
							ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
							ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING))));
			
			gpsAcc.setText(Integer.toString(mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 
					ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)) + " " + getActivity().getResources().getString(R.string.meters));
			
			break;
			
	
		default:
			break;
		}
		
	}
	

	public void afterTextChanged(Editable s) {
		Log.e("days", s.toString());
		

		
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}
	
}
