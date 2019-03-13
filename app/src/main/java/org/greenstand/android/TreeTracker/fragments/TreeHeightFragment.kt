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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.fragment_tree_height.view.*
import org.greenstand.android.TreeTracker.R




class TreeHeightFragment : Fragment() {

    lateinit var viewModel: TreeHeightViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(TreeHeightViewModelImpl::class.java)
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tree_height, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        val colorDrawable: ColorDrawable = view.height_button_five.background as ColorDrawable

        val toColor = context!!.resources.getColor(R.color.error_color_material_light)

        // NOTE TO SELF
        // USE constraint layout sets to make a chain (the 5 colors)
        // make a colored view that animates to different positions (code in constraints)
        // make a single text that also has dynamic constraints

        // biases 1->0, 2->.275, 3->.5, 4->0.725, 5->1

        view.post {

            val height = view.stick_container.height / 5
            val width = view.stick_container.width

            val selectedHeight = (height * 1.1).toInt()
            val selectedWidth = (width * 2).toInt()

            ConstraintSet().apply {
                clone(view as ConstraintLayout)
                connect(R.id.floating_button, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32)
                connect(R.id.floating_button, ConstraintSet.BOTTOM, R.id.fragmentTreeHeightSave, ConstraintSet.TOP, 32)
                centerHorizontally(R.id.floating_button, ConstraintSet.PARENT_ID)
                setVerticalBias(R.id.floating_button, 0.725f)
                constrainHeight(R.id.floating_button, selectedHeight)
                constrainWidth(R.id.floating_button, selectedWidth)
                applyTo(view)
            }

        }


        listOf(view.height_button_one,
               view.height_button_two,
               view.height_button_three,
               view.height_button_four,
               view.height_button_five)
            .map { it to (it.background as ColorDrawable).color }
            .forEach { (heightView, color) ->
                heightView.setOnClickListener {
                    animateColors(it, color, toColor)
                }
            }
    }

    fun animateColors(view: View, fromColor: Int, toColor: Int) {
        val stringFromColor = String.format("#%06X", 0xFFFFFF and fromColor)
        val stringToColor = String.format("#%06X", 0xFFFFFF and toColor)

        val from = FloatArray(3)
        val to = FloatArray(3)

        Color.colorToHSV(Color.parseColor(stringFromColor), from)
        Color.colorToHSV(Color.parseColor(stringToColor), to)

        val anim = ValueAnimator.ofFloat(0F, 1F)
        anim.duration = 1000

        val hsv = FloatArray(3)
        anim.addUpdateListener { animation ->
            // Transition along each axis of HSV (hue, saturation, value)
            hsv[0] = from[0] + (to[0] - from[0]) * animation.animatedFraction
            hsv[1] = from[1] + (to[1] - from[1]) * animation.animatedFraction
            hsv[2] = from[2] + (to[2] - from[2]) * animation.animatedFraction

            view.setBackgroundColor(Color.HSVToColor(hsv))
        }

        anim.start()
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