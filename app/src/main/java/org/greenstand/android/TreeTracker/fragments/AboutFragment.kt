package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_about.*

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R


class AboutFragment : androidx.fragment.app.Fragment(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.toolbarTitle?.setText(R.string.information)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        fragmentAboutVersionCode.text = "${getString(R.string.build_version_title)} ${BuildConfig.VERSION_CODE}"
        fragmentAboutVersionName.text = "${getString(R.string.tree_tracker_title)} ${BuildConfig.VERSION_NAME}"
    }

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

}
