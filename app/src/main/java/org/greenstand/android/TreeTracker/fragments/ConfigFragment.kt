package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.toolbarTitle
import kotlinx.android.synthetic.main.fragment_config.convergenceDataSizeVal
import kotlinx.android.synthetic.main.fragment_config.convergenceTimeoutVal
import kotlinx.android.synthetic.main.fragment_config.latStdDevThresholdVal
import kotlinx.android.synthetic.main.fragment_config.locationDataConfig
import kotlinx.android.synthetic.main.fragment_config.lonStdDevThresholdVal
import kotlinx.android.synthetic.main.fragment_config.minDisBtwnUpdatesVal
import kotlinx.android.synthetic.main.fragment_config.minTimeBtwnUpdatesVal
import kotlinx.android.synthetic.main.fragment_config.saveConfigButton
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.databinding.FragmentAdminLoginBinding
import org.greenstand.android.TreeTracker.databinding.FragmentConfigBinding
import org.greenstand.android.TreeTracker.models.LocationDataConfig
import org.greenstand.android.TreeTracker.viewmodels.ConfigViewModel
import org.koin.android.ext.android.inject

class ConfigFragment : Fragment() {

    private lateinit var bindings: FragmentConfigBinding
    private val configViewModel by inject<ConfigViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentConfigBinding.inflate(inflater)
        (requireActivity() as MainActivity).apply {
            bindings.toolbarTitle.setText(R.string.config_parameters)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configViewModel
            .getLocationDataConfig()
            .observe(
                viewLifecycleOwner,
                { locationDataConfig ->
                    minTimeBtwnUpdatesVal.setText(
                        locationDataConfig.minTimeBetweenUpdates.toString()
                    )
                    minDisBtwnUpdatesVal.setText(
                        locationDataConfig.minDistanceBetweenUpdates.toString()
                    )
                    convergenceTimeoutVal.setText(locationDataConfig.convergenceTimeout.toString())
                    convergenceDataSizeVal.setText(
                        locationDataConfig.convergenceDataSize.toString()
                    )
                    lonStdDevThresholdVal.setText(locationDataConfig.lonStdDevThreshold.toString())
                    latStdDevThresholdVal.setText(locationDataConfig.latStdDevThreshold.toString())
                }
            )

        saveConfigButton.setOnClickListener {
            configViewModel.updateLocationDataConfig(
                LocationDataConfig(
                    minTimeBetweenUpdates = minTimeBtwnUpdatesVal.text.toString().toLong(),
                    minDistanceBetweenUpdates = minDisBtwnUpdatesVal.text.toString().toFloat(),
                    convergenceTimeout = convergenceTimeoutVal.text.toString().toLong(),
                    convergenceDataSize = convergenceDataSizeVal.text.toString().toInt(),
                    lonStdDevThreshold = lonStdDevThresholdVal.text.toString().toFloat(),
                    latStdDevThreshold = latStdDevThresholdVal.text.toString().toFloat()
                )
            )
            val toast = Toast.makeText(activity, "Configuration Saved !!", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }
}
