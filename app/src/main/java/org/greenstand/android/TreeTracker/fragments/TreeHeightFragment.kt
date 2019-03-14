package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlin.coroutines.CoroutineContext
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.fragment_tree_height.*
import kotlinx.android.synthetic.main.fragment_tree_height.view.*
import org.greenstand.android.TreeTracker.R

import android.animation.ArgbEvaluator


class TreeHeightFragment : Fragment() {

    lateinit var viewModel: TreeHeightViewModel

    var isInitialState = true
    var lastColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(TreeHeightViewModel::class.java)
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tree_height, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val parentView = view as ConstraintLayout

        // NOTE TO SELF
        // USE constraint layout sets to make a chain (the 5 colors)
        // make a colored view that animates to different positions (code in constraints)
        // make a single text that also has dynamic constraints

        // biases 1->0, 2->.275, 3->.5, 4->0.725, 5->1


        listOf(
               view.height_button_five,
               view.height_button_four,
               view.height_button_three,
               view.height_button_two,
               view.height_button_one
        )
            //.map { it to (it.background as ColorDrawable).color }
            .forEachIndexed { index, view ->
                view.setOnClickListener {
                    moveToHeight(parentView, view, index)
                    //animateColors(it, color, toColor)
                }
            }
    }
    // biases 1->0, 2->.275, 3->.5, 4->0.725, 5->1
    fun indexToBias(index: Int): Float {
        return when(index) {
            0 -> .025f
            1 -> .275f
            2 -> .5f
            3 -> .725f
            4 -> .975f
            else -> 0f
        }
    }

    fun moveToHeight(view: ConstraintLayout, colorView: View, index: Int) {

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
                    connect(R.id.floating_button, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32)
                    connect(R.id.floating_button, ConstraintSet.BOTTOM, R.id.fragmentTreeHeightSave, ConstraintSet.TOP, 32)
                    centerHorizontally(R.id.floating_button, ConstraintSet.PARENT_ID)
                    setVerticalBias(R.id.floating_button, bias)
                    constrainHeight(R.id.floating_button, selectedHeight)
                    constrainWidth(R.id.floating_button, selectedWidth)
                    colorLerp(floating_button, floating_button.color, colorView.color)
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
                    connect(R.id.floating_button, ConstraintSet.BOTTOM, R.id.fragmentTreeHeightSave, ConstraintSet.TOP, 32)
                    centerHorizontally(R.id.floating_button, ConstraintSet.PARENT_ID)
                    setVerticalBias(R.id.floating_button, bias)
                    constrainHeight(R.id.floating_button, height)
                    constrainWidth(R.id.floating_button, width)
                    setVisibility(R.id.floating_button, ConstraintSet.VISIBLE)
                    floating_button.setCardBackgroundColor(colorView.color)
                    applyTo(view)
                }
                isInitialState = false

                animatedUpdate(view, index)
            }
        } else {
            animatedUpdate(view, index)
        }
    }

    val View.color: Int
        get() {
            val back = background
            return when(back) {
                is ColorDrawable -> back.color
                is ColorStateList -> (back.current as ColorDrawable).color
                else -> 0
            }
        }



    fun colorLerp(view: View, colorFrom: Int, colorTo: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 300 // milliseconds
            addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
            start()
        }
    }

}


class TreeHeightViewModel : CoroutineViewModel() {



}




abstract class CoroutineViewModel : ViewModel(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = UI

    override fun onCleared() {
        job.cancel()
    }
}