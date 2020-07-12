package org.greenstand.android.TreeTracker.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_tree_preview.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.viewmodels.TreePreviewViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

class TreePreviewFragment : Fragment() {

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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tree_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(requireActivity() as AppCompatActivity) {
            toolbarTitle?.setText(R.string.tree_preview)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        lifecycleScope.launchWhenCreated {
            val treeData = vm.loadTree(args.treeId.toLong())

            if (treeData.localPhotoPath != null) {
                val bitmap = ImageUtils.decodeBitmap(treeData.localPhotoPath, resources.displayMetrics.density)
                fragmentTreePreviewImage.setImageBitmap(bitmap)
                fragmentTreePreviewImage.visibility = View.VISIBLE
                fragmentTreePreviewNoImage.visibility = View.INVISIBLE
            } else {
                fragmentTreePreviewNoImage.visibility = View.VISIBLE
            }

            fragmentTreePreviewDistance.text = "${treeData.distance.toInt()} ${resources.getString(R.string.meters)}"
            fragmentTreePreviewGpsAccuracy.text = "${treeData.accuracy} ${resources.getString(R.string.meters)}"
            fragmentTreePreviewCreated.text = Date(treeData.createdAt).toLocaleString()
            fragmentTreePreviewNotes.text = treeData.note
        }
    }
}
