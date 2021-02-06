package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.FragmentOrgWallBinding

class OrgWallFragment : Fragment() {

    private lateinit var bindings: FragmentOrgWallBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentOrgWallBinding.inflate(inflater)
        return bindings.root
    }
}
