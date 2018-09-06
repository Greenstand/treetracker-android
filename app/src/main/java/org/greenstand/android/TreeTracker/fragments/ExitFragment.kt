package org.greenstand.android.TreeTracker.fragments


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button

import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class ExitFragment : Fragment(), OnClickListener {

    private var fragment: Fragment? = null
    private var bundle: Bundle? = null
    private var fragmentTransaction: FragmentTransaction? = null
    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater!!.inflate(R.layout.fragment_exit, container, false)

        mSharedPreferences = activity.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        val yesBtn = v.findViewById(R.id.fragment_exit_yes) as Button
        yesBtn.setOnClickListener(this@ExitFragment)

        val noBtn = v.findViewById(R.id.fragment_exit_no) as Button
        noBtn.setOnClickListener(this@ExitFragment)

        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragment_exit_yes -> {

                MainActivity.syncDataFromExitScreen = true

                fragment = DataFragment()
                bundle = activity.intent.extras
                fragment!!.arguments = bundle

                fragmentTransaction = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction!!.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit()

                val manager = activity.supportFragmentManager
                val first = manager.getBackStackEntryAt(0)
                manager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            R.id.fragment_exit_no -> activity.finish()
        }


    }


}//some overrides and settings go here
