package org.greenstand.android.TreeTracker.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenstand.android.TreeTracker.BuildConfig;
import org.greenstand.android.TreeTracker.R;

public class AboutFragment extends Fragment implements OnClickListener {

    private Fragment fragment;
    private Bundle bundle;
    private FragmentTransaction fragmentTransaction;
    private SharedPreferences mSharedPreferences;

    private int versionCode;
    private String versionName;
    private String versionCode_string;
    private TextView versioncode;

    private TextView versionname;



    public AboutFragment() {
        //some overrides and settings go here
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //getting version code and version name
        versionCode = BuildConfig.VERSION_CODE;
        versionName= BuildConfig.VERSION_NAME;
        versionCode_string=Integer.toString(versionCode);




    }

    @Override
    public void onResume() {
        super.onResume();
        //updateTextView();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_about, container, false);
        //	    ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getActivity().getResources().getString(R.string.information));
//	    ((ActionBarActivity)getActivity()).getSupportActionBar().show();


        ((TextView) getActivity().findViewById(R.id.toolbar_title)).setText(R.string.information);
//		((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.about);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        versioncode = (TextView) v.findViewById(R.id.fragment_about_versioncode);
        versionname = (TextView) v.findViewById(R.id.fragment_about_versionname);

       //setting version code and versionname
        versioncode.setText("Build version  "+versionCode_string);
        versionname.setText("Tree Tracker(debug) "+versionName);



            return v;
        }

    public void onClick(View v) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);


    }



}
