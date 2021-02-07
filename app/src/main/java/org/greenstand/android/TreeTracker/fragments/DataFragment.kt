package org.greenstand.android.TreeTracker.fragments


import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.FragmentDataBinding
import org.greenstand.android.TreeTracker.utilities.mainActivity
import org.greenstand.android.TreeTracker.viewmodels.DataViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class DataFragment : Fragment() {

    private lateinit var bindings: FragmentDataBinding

    private val viewModel: DataViewModel by viewModel()

    private val args: DataFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args.startSync) {
            viewModel.sync()
        }

        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindings = FragmentDataBinding.inflate(inflater)

        mainActivity().bindings.toolbarTitle.setText(R.string.data)
        mainActivity().supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.treeData.observe(this, Observer { treeData ->
            bindings.fragmentDataTotalTreesValue.text = treeData.totalTrees.toString()
            bindings.fragmentDataLocatedValue.text = treeData.treesSynced.toString()
            bindings.fragmentDataToSyncValue.text = treeData.treesToSync.toString()
        })

        viewModel.toasts.observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.isSyncing.observe(this, Observer { isSyncing ->
            val textId = if (isSyncing) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                R.string.stop
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                R.string.sync
            }
            bindings.fragmentDataSyncButton.setText(textId)
        })

        bindings.fragmentDataSyncButton.setOnClickListener {
            viewModel.sync()
        }
    }
}
