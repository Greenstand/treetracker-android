package org.greenstand.android.TreeTracker.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
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
import java.util.*

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
        viewModel.planterAccountData.observe(this, Observer { planterAccountData ->
            val currencySymbol = Currency.getInstance(planterAccountData.paymentCurrencyCode).symbol
            fragmentDataUploaded.text = planterAccountData.uploadedCount.toString()
            fragmentDataWaitingUpload.text = planterAccountData.waitingToUploadCount.toString()
            fragmentDataValidatedTrees.text = planterAccountData.validatedCount.toString()
            paymentPendingValue.text = "$currencySymbol${planterAccountData.paymentAmountPending.toString()}"
            totalPaidValue.text = "$currencySymbol${planterAccountData.totalAmountPaid.toString()}"
        })

        viewModel.toasts.observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.isSyncing.observe(this, Observer { isSyncing ->
            if (isSyncing) {
                fragmentDataSyncButton.setImageResource(R.drawable.stop_40dp)
            } else {
                fragmentDataSyncButton.setImageResource(R.drawable.cloud_backup_40)
            }
        })

        fragmentDataSyncButton.setOnClickListener {
            viewModel.sync()
        }
    }
}
