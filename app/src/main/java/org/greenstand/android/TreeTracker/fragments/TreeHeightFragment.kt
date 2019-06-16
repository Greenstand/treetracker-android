package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.fragment_tree_height.*
import kotlinx.android.synthetic.main.fragment_tree_height.view.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.data.TreeColor
import org.greenstand.android.TreeTracker.utilities.ValueHelper

import org.greenstand.android.TreeTracker.utilities.animateColor
import org.greenstand.android.TreeTracker.utilities.color
import org.greenstand.android.TreeTracker.viewmodels.TreeHeightViewModel
import org.koin.android.viewmodel.ext.android.viewModel


class TreeHeightFragment : Fragment() {

    private val vm: TreeHeightViewModel by viewModel()
    private val args: TreeHeightFragmentArgs by navArgs()
    private var isInitialState = true


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tree_height, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val parentView = view as ConstraintLayout

        vm.newTree = args.newTree

        listOf(height_button_five,
               height_button_four,
               height_button_three,
               height_button_two,
               height_button_one)
            .forEachIndexed { index, colorView ->
                colorView.setOnClickListener {
                    moveSelection(parentView, colorView, index)
                    vm.treeColor = indexToTreeColor(index)
                }
            }

        save_tree_height.setOnClickListener {
            vm.saveNewTree()
        }

        vm.toastMessagesLiveData().observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        vm.onFinishedLiveData().observe(this, Observer {
            findNavController().popBackStack(R.id.mapsFragment, false)
            fragmentManager?.popBackStack(ValueHelper.NEW_TREE_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        })

        vm.onEnableButtonLiveData().observe(this, Observer {
            save_tree_height.isEnabled = it
        })
    }

    private fun indexToBias(index: Int): Float {
        return when(index) {
            0 -> .025f
            1 -> .275f
            2 -> .5f
            3 -> .725f
            4 -> .975f
            else -> 0f
        }
    }

    private fun indexToTreeColor(index: Int): TreeColor {
        return when(index) {
            0 -> TreeColor.GREEN
            1 -> TreeColor.PURPLE
            2 -> TreeColor.YELLOW
            3 -> TreeColor.BLUE
            4 -> TreeColor.ORANGE
            else -> TreeColor.GREEN
        }
    }

    private fun moveSelection(view: ConstraintLayout, colorView: View, index: Int) {

        fun animatedUpdate(view: ConstraintLayout, index: Int) {
            view.post {

                val bias = indexToBias(index)

                val height = view.stick_container.height / 5
                val width = view.stick_container.width

                val selectedHeight = (height * 1.1).toInt()
                val selectedWidth = (width * 2)

                TransitionManager.beginDelayedTransition(view)

                ConstraintSet().apply {
                    clone(view)
                    setVerticalBias(R.id.floating_button, bias)
                    constrainHeight(R.id.floating_button, selectedHeight)
                    constrainWidth(R.id.floating_button, selectedWidth)
                    floating_button_inner.animateColor(toColor = colorView.color)
                    applyTo(view)
                }

            }
        }

        if (isInitialState) {
            // move view to position of tapped color, size it, color it, make visible all without animations
            view.post {

                val bias = indexToBias(index)

                val height = view.stick_container.height / 5
                val width = view.stick_container.width

                ConstraintSet().apply {
                    clone(view)
                    connect(R.id.floating_button, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32)
                    connect(R.id.floating_button, ConstraintSet.BOTTOM, R.id.save_tree_height, ConstraintSet.TOP, 32)
                    centerHorizontally(R.id.floating_button, ConstraintSet.PARENT_ID)
                    setVerticalBias(R.id.floating_button, bias)
                    constrainHeight(R.id.floating_button, height)
                    constrainWidth(R.id.floating_button, width)
                    setVisibility(R.id.floating_button, ConstraintSet.VISIBLE)
                    floating_button_inner.setBackgroundColor(colorView.color)
                    applyTo(view)
                }
                isInitialState = false

                animatedUpdate(view, index)
            }
        } else {
            animatedUpdate(view, index)
        }
    }

}

