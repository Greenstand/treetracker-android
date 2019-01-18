package org.greenstand.android.TreeTracker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_about.*

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R


class AboutFragment : androidx.fragment.app.Fragment(), OnClickListener {

    private val fragment: androidx.fragment.app.Fragment? = null
    private val bundle: Bundle? = null
    private val fragmentTransaction: androidx.fragment.app.FragmentTransaction? = null
    private val mSharedPreferences: SharedPreferences? = null

    private var versionCode: Int = 0
    private var versionName: String? = null
    private var versionCode_string: String? = null
    private var versioncode: TextView? = null

    private var versionname: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        //getting version code and version name
        versionCode = BuildConfig.VERSION_CODE
        versionName = BuildConfig.VERSION_NAME
        versionCode_string = Integer.toString(versionCode)


    }

    override fun onResume() {
        super.onResume()
        //updateTextView();
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_about, container, false)
        //	    ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getActivity().getResources().getString(R.string.information));
        //	    ((ActionBarActivity)getActivity()).getSupportActionBar().show();


        activity?.toolbarTitle?.setText(R.string.information)
        //		((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.about);
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        versioncode = fragmentAboutVersioncode
        versionname = fragmentAboutVersionname

        //setting version code and versionname
        versioncode?.text = "Build version  " + versionCode_string!!
        versionname?.text = "Tree Tracker(debug) " + versionName!!



        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)


    }


}//some overrides and settings go here
