package com.qalliance.treetracker.TreeTracker.fragments;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.SupportMapFragment;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

public class HomeFragment extends Fragment implements OnClickListener {
	
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;
	private SharedPreferences mSharedPreferences;

	public HomeFragment() {
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
		
	    View v = inflater.inflate(R.layout.fragment_home, container, false);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(
	      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);

//	    ((TextView)getActivity().findViewById(R.id.actionbar_title)).setText(R.string.welcome);

	    ((ActionBarActivity)getActivity()).getSupportActionBar().show();
	    
	    Button mapBtn = (Button) v.findViewById(R.id.fragment_home_map);
	    mapBtn.setOnClickListener(HomeFragment.this);

	    Button dataBtn = (Button) v.findViewById(R.id.fragment_home_data);
	    dataBtn.setOnClickListener(HomeFragment.this);
	    
	    Button settingsBtn = (Button) v.findViewById(R.id.fragment_home_settings);
	    settingsBtn.setOnClickListener(HomeFragment.this);
	    
	    Button exitBtn = (Button) v.findViewById(R.id.fragment_home_exit);
	    exitBtn.setOnClickListener(HomeFragment.this);
	    
	    TextView welcomeTxt = (TextView) v.findViewById(R.id.fragment_home_welcome);
	    welcomeTxt.setText(welcomeTxt.getText() + " " + mSharedPreferences.getString(ValueHelper.MAIN_USER_FIRST_NAME, ""));
	    
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_home_map:

			    SupportMapFragment mapFragment = ((SupportMapFragment) getActivity()
			            .getSupportFragmentManager().findFragmentById(R.id.map));

			    if(mapFragment != null) {
			    	getActivity().getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
			    }
				
				fragment = new MapsFragment();
				bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);
				
				fragmentTransaction = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.MAP_FRAGMENT).commit();
				break;
			case R.id.fragment_home_data:
				fragment = new DataFragment();
				bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);
				
				fragmentTransaction = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
				break;				
			case R.id.fragment_home_settings:
				fragment = new SettingsFragment();
				bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);
				
				fragmentTransaction = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.SETTINGS_FRAGMENT).commit();
				break;		
			case R.id.fragment_home_exit:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				 
				builder.setTitle(R.string.exit);
				builder.setMessage(R.string.do_you_want_to_sync_your_data_now);
				 
				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				 
				   public void onClick(DialogInterface dialog, int which) {

					   	MainActivity.syncDataFromExitScreen = true;
					   
						fragment = new DataFragment();
						bundle = getActivity().getIntent().getExtras();
						fragment.setArguments(bundle);
						
						fragmentTransaction = getActivity().getSupportFragmentManager()
								.beginTransaction();
						fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
						
				        dialog.dismiss();
				   }
				 
				});
				 
				 
				builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				 
				   public void onClick(DialogInterface dialog, int which) {
				 
				        // Code that is executed when clicking NO
				 
					   	getActivity().finish();
					   
				        dialog.dismiss();
				   }
				 
				});
				 
				 
				AlertDialog alert = builder.create();
				alert.show();
				break;			
		}

		
	}


}
