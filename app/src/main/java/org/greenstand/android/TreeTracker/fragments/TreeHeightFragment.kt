package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import org.greenstand.android.TreeTracker.R
import kotlin.coroutines.CoroutineContext

class TreeHeightFragment : Fragment() {

    lateinit var viewModel: TreeHeightViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(TreeHeightViewModelImpl::class.java)
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tree_height, container, false)
    }



}



interface TreeHeightViewModel {

}

class TreeHeightViewModelImpl : CoroutineViewModel(), TreeHeightViewModel {



}




abstract class CoroutineViewModel : ViewModel(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = UI

    override fun onCleared() {
        job.cancel()
    }
}