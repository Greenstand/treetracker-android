package org.greenstand.android.TreeTracker.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import java.util.Date
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.FragmentTreePreviewBinding
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.mainActivity
import org.greenstand.android.TreeTracker.viewmodels.TreePreviewViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class TreePreviewFragment : Fragment() {

    private lateinit var bindings: FragmentTreePreviewBinding

    private val vm: TreePreviewViewModel by viewModel()
    private val args: TreePreviewFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentTreePreviewBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(mainActivity()) {
            bindings.toolbarTitle.setText(R.string.tree_preview)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        lifecycleScope.launchWhenCreated {
            val treeData = vm.loadTree(args.treeId.toLong())

            if (treeData.localPhotoPath != null) {
                val bitmap = ImageUtils.decodeBitmap(
                    treeData.localPhotoPath, resources.displayMetrics.density
                )
                bindings.fragmentTreePreviewImage.setImageBitmap(bitmap)
                bindings.fragmentTreePreviewImage.visibility = View.VISIBLE
                bindings.fragmentTreePreviewNoImage.visibility = View.INVISIBLE
            } else {
                bindings.fragmentTreePreviewNoImage.visibility = View.VISIBLE
            }

            bindings.fragmentTreePreviewDistance.text = "${treeData.distance.toInt()} ${resources.getString(R.string.meters)}"
            bindings.fragmentTreePreviewCreated.text = Date(treeData.createdAt).toLocaleString()
            bindings.fragmentTreePreviewNotes.text = treeData.note
        }
    }
}
