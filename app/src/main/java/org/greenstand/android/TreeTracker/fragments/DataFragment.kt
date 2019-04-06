package org.greenstand.android.TreeTracker.fragments


import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.amazonaws.AmazonClientException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_data.*
import kotlinx.android.synthetic.main.fragment_data.view.*
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.api.models.requests.*
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.dao.TreeDto
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.DataViewModel
import org.greenstand.android.TreeTracker.viewmodels.TreeHeightViewModel
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Integer.valueOf

class DataFragment : Fragment() {

    private var progressDialog: ProgressDialog? = null
    private var operationAttempt: Job? = null

    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(DataViewModel::class.java)
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_data, container, false)

        activity?.toolbarTitle?.setText(R.string.data)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.totalTrees.observe(this, Observer {
            fragmentDataTotalTreesValue.text = it.toString()
        })

        viewModel.treesUploaded.observe(this, Observer {
            fragmentDataLocatedValue.text = it.toString()
        })

        viewModel.treesToSync.observe(this, Observer {
            fragmentDataToSyncValue.text = it.toString()
        })

        viewModel.toasts.observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        fragmentDataSync.setOnClickListener {
            viewModel.sync()
        }
    }

    override fun onResume() {
        super.onResume()

        val extras = arguments
        if (extras != null) {
            if (extras.getBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN)) {
                progressDialog = ProgressDialog(activity)
                progressDialog!!.setCancelable(false)
                progressDialog!!.setMessage(activity!!.getString(R.string.downloading_your_trees))
                progressDialog!!.show()
            }

            if (extras.getBoolean(ValueHelper.RUN_FROM_NOTIFICATION_SYNC)) {
                Toast.makeText(activity, R.string.sync_started, Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        if (operationAttempt != null) {
            operationAttempt!!.cancel()
            Toast.makeText(activity, R.string.sync_stopped, Toast.LENGTH_SHORT).show()
        }
    }

}
