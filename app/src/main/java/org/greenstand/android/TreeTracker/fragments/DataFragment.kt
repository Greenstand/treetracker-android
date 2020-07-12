package org.greenstand.android.TreeTracker.fragments


import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_data.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.viewmodels.DataViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class DataFragment : Fragment() {

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
        val v = inflater.inflate(R.layout.fragment_data, container, false)

        requireActivity().toolbarTitle?.setText(R.string.data)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.treeData.observe(this, Observer { treeData ->
            fragmentDataTotalTreesValue.text = treeData.totalTrees.toString()
            fragmentDataLocatedValue.text = treeData.treesSynced.toString()
            fragmentDataToSyncValue.text = treeData.treesToSync.toString()
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
            fragmentDataSyncButton.setText(textId)
        })

        fragmentDataSyncButton.setOnClickListener {
            viewModel.sync()
        }
    }
}
