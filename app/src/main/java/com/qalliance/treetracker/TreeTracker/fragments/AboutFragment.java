package com.qalliance.treetracker.TreeTracker.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qalliance.treetracker.TreeTracker.R;

public class AboutFragment extends Fragment implements OnClickListener {
	
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;

	public AboutFragment() {
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
		
	    View v = inflater.inflate(R.layout.fragment_about, container, false);
	    
//	    ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getActivity().getResources().getString(R.string.information));
//	    ((ActionBarActivity)getActivity()).getSupportActionBar().show();
	    
	    ((TextView)getActivity().findViewById(R.id.actionbar_title)).setText(R.string.information);
	    
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		
	}


}
