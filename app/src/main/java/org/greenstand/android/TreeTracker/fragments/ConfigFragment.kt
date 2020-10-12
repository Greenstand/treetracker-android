package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.toolbarTitle
import kotlinx.android.synthetic.main.fragment_config.locationDataConfig
import kotlinx.android.synthetic.main.fragment_config.minTimeBtwnUpdatesVal
import kotlinx.android.synthetic.main.fragment_config.saveConfigButton
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.LocationDataConfig
import org.greenstand.android.TreeTracker.viewmodels.ConfigViewModel
import org.koin.android.ext.android.inject

class ConfigFragment : Fragment() {

    private val configViewModel by inject<ConfigViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.toolbarTitle?.setText(R.string.config_parameters)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configViewModel
            .getLocationDataConfig()
            .observe(viewLifecycleOwner, androidx.lifecycle.Observer { locationDataConfig ->
                minTimeBtwnUpdatesVal.setText(locationDataConfig.minTimeBetweenUpdates.toString())
            })

        saveConfigButton.setOnClickListener {
            configViewModel.updateLocationDataConfig(LocationDataConfig(
                minTimeBetweenUpdates = minTimeBtwnUpdatesVal.text.toString().toLong()
            ))
        }
    }
}
