package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.databinding.FragmentAboutBinding
import org.greenstand.android.TreeTracker.utilities.vibrate

class AboutFragment : androidx.fragment.app.Fragment(), OnClickListener {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).apply {
            bindings.toolbarTitle.setText(R.string.information)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        binding.fragmentAboutVersionCode.text = "${getString(R.string.build_version_title)}" +
            " ${BuildConfig.VERSION_CODE}"
        binding.fragmentAboutVersionName.text = "${getString(R.string.tree_tracker_title)}" +
            " ${BuildConfig.VERSION_NAME}"

        binding.fragmentAboutVersionCode.setOnTouchListener { v, event ->
            if (view.findNavController().currentDestination?.id == R.id.aboutFragment) {
                findNavController()
                    .navigate(AboutFragmentDirections.actionAboutFragmentToAdminLoginFragment())
            }
            true
        }
    }

    override fun onClick(v: View) {
        v.vibrate()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}
