package org.greenstand.android.TreeTracker.fragments


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import android.widget.TextView
import android.widget.Toast

import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

class SettingsFragment : Fragment(), OnClickListener, OnCheckedChangeListener, TextWatcher {

    private val fragment: Fragment? = null
    private val bundle: Bundle? = null
    private val fragmentTransaction: FragmentTransaction? = null
    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        mSharedPreferences = activity!!.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        (activity!!.findViewById(R.id.toolbar_title) as TextView).setText(R.string.settings)
        //		((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val whichSettings = v.findViewById(R.id.fragment_settings_which_settings) as RadioGroup
        whichSettings.setOnCheckedChangeListener(this@SettingsFragment)

        val manualRadioSettings = v.findViewById(R.id.fragment_settings_manual_settings_radio_group) as RadioGroup
        manualRadioSettings.setOnCheckedChangeListener(this@SettingsFragment)

        val saveAndEdit = v.findViewById(R.id.fragment_settings_save_and_edit) as CheckBox
        saveAndEdit.isChecked = mSharedPreferences!!.getBoolean(ValueHelper.SAVE_AND_EDIT, false)

        val nextUpdate = v.findViewById(R.id.fragment_settings_next_update) as EditText
        nextUpdate.addTextChangedListener(this@SettingsFragment)

        nextUpdate.setText(Integer.toString(mSharedPreferences!!.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences!!.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING))))

        val gpsAcc = v.findViewById(R.id.fragment_settings_gps_accuracy) as TextView
        if (mSharedPreferences!!.getBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)) {
            whichSettings.check(R.id.fragment_settings_treetracker)


            val accServer = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)
            val nextUpdateServer = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING)

            mSharedPreferences!!.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, accServer).commit()
            mSharedPreferences!!.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, nextUpdateServer).commit()

            //TODO replace with data from server
            nextUpdate.setText(Integer.toString(nextUpdateServer))

            gpsAcc.text = Integer.toString(accServer) + " " + (activity as AppCompatActivity).resources.getString(R.string.meters)

            nextUpdate.isEnabled = false
        } else {
            val manualSettings = v.findViewById(R.id.fragment_settings_manual_settings) as LinearLayout
            whichSettings.check(R.id.fragment_settings_manual)
            manualSettings.visibility = View.VISIBLE
            nextUpdate.isEnabled = true

            gpsAcc.visibility = View.INVISIBLE
            saveAndEdit.visibility = View.VISIBLE
        }


        for (i in 0 until manualRadioSettings.childCount) {
            val rb = manualRadioSettings.getChildAt(i) as RadioButton

            if (rb != null) {


                if (Integer.parseInt(rb.text.toString()) == mSharedPreferences!!.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)) {
                    manualRadioSettings.check(rb.id)
                    break
                }
            }
        }

        val submitBtn = v.findViewById(R.id.fragment_settings_submit) as Button
        submitBtn.setOnClickListener(this@SettingsFragment)

        for (i in 0 until manualRadioSettings.childCount) {
            (manualRadioSettings.getChildAt(i) as RadioButton).text = ((manualRadioSettings.getChildAt(i) as RadioButton).text.toString()
                    + " "
                    + (activity as AppCompatActivity).resources.getString(R.string.meters))
        }

        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragment_settings_submit -> {

                val radioGroup = activity!!.findViewById(R.id.fragment_settings_which_settings) as RadioGroup
                val saveAndEdit = activity!!.findViewById(R.id.fragment_settings_save_and_edit) as CheckBox

                if (radioGroup.checkedRadioButtonId == R.id.fragment_settings_treetracker) {
                    mSharedPreferences!!.edit().putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true).commit()

                    val acc = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)
                    val nextUpdate = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING)

                    mSharedPreferences!!.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, acc).commit()
                    mSharedPreferences!!.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, nextUpdate).commit()

                } else {
                    mSharedPreferences!!.edit().putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, false).commit()


                    val nextUpdate = activity!!.findViewById(R.id.fragment_settings_next_update) as EditText

                    mSharedPreferences!!.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, Integer.parseInt(nextUpdate.text.toString())).commit()

                    val accuracyRadioGroup = activity!!.findViewById(R.id.fragment_settings_manual_settings_radio_group) as RadioGroup
                    val selectedRadioButton = activity!!.findViewById(accuracyRadioGroup.checkedRadioButtonId) as RadioButton

                    if (selectedRadioButton != null) {
                        mSharedPreferences!!.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
                                Integer.parseInt(selectedRadioButton.text.toString().substring(0, selectedRadioButton.text.toString().lastIndexOf(" ")))).commit()
                    }

                    if (saveAndEdit.isChecked) {
                        mSharedPreferences!!.edit().putBoolean(ValueHelper.SAVE_AND_EDIT, true).commit()
                    } else {
                        mSharedPreferences!!.edit().putBoolean(ValueHelper.SAVE_AND_EDIT, false).commit()
                    }


                }

                Toast.makeText(activity, activity!!.resources.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                activity!!.supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onCheckedChanged(radiogroup: RadioGroup, checkedId: Int) {
        val manualSettings = activity!!.findViewById(R.id.fragment_settings_manual_settings) as LinearLayout
        val nextUpdate = activity!!.findViewById(R.id.fragment_settings_next_update) as EditText
        val gpsAcc = activity!!.findViewById(R.id.fragment_settings_gps_accuracy) as TextView
        val saveAndEdit = activity!!.findViewById(R.id.fragment_settings_save_and_edit) as CheckBox


        if (manualSettings == null) {
            return
        }

        when (checkedId) {
            R.id.fragment_settings_treetracker -> {
                manualSettings.visibility = View.GONE
                saveAndEdit.visibility = View.GONE
                nextUpdate.isEnabled = false
                gpsAcc.visibility = View.VISIBLE

                val accServer = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_MIN_ACCURACY, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)
                val nextUpdateServer = mSharedPreferences!!.getInt(ValueHelper.MAIN_DB_NEXT_UPDATE, ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING)

                nextUpdate.setText(Integer.toString(accServer))
                gpsAcc.text = Integer.toString(nextUpdateServer) + " " + activity!!.resources.getString(R.string.meters)
            }

            R.id.fragment_settings_manual -> {
                manualSettings.visibility = View.VISIBLE
                saveAndEdit.visibility = View.VISIBLE
                nextUpdate.isEnabled = true
                gpsAcc.visibility = View.INVISIBLE

                nextUpdate.setText(Integer.toString(mSharedPreferences!!.getInt(
                        ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences!!.getInt(
                        ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                        ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING))))

                gpsAcc.text = Integer.toString(mSharedPreferences!!.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
                        ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)) + " " + activity!!.resources.getString(R.string.meters)
            }


            else -> {
            }
        }

    }


    override fun afterTextChanged(s: Editable) {
        Timber.d("days " + s.toString())
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int,
                                   after: Int) {
        // TODO Auto-generated method stub

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // TODO Auto-generated method stub

    }

}//some overrides and settings go here
