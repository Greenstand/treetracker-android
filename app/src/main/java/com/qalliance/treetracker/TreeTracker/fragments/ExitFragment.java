package com.qalliance.treetracker.TreeTracker.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

public class ExitFragment extends Fragment implements OnClickListener {
	
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;
	private SharedPreferences mSharedPreferences;

	public ExitFragment() {
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
		
	    View v = inflater.inflate(R.layout.fragment_exit, container, false);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(
	      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);

	    Button yesBtn = (Button) v.findViewById(R.id.fragment_exit_yes);
	    yesBtn.setOnClickListener(ExitFragment.this);

	    Button noBtn = (Button) v.findViewById(R.id.fragment_exit_no);
	    noBtn.setOnClickListener(ExitFragment.this);
	    
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_exit_yes:

				MainActivity.syncDataFromExitScreen = true;
				
				fragment = new DataFragment();
				bundle = getActivity().getIntent().getExtras();
				fragment.setArguments(bundle);
				
				fragmentTransaction = getActivity().getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
				
				FragmentManager manager = getActivity().getSupportFragmentManager();
				FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(0);
				manager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
				
				break;		
			case R.id.fragment_exit_no:
				getActivity().finish();
				break;					
		}

		
	}


}
