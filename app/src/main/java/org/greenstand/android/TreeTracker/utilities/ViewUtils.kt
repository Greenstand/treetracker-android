package org.greenstand.android.TreeTracker.utilities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.card.MaterialCardView
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity

fun Fragment.mainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun View.animateColor(toColor: Int, fromColor: Int = color, durationMsec: Long = 300) {
    ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
        duration = durationMsec
        addUpdateListener { animator -> setBackgroundColor(animator.animatedValue as Int) }
        start()
    }
}

val View.color: Int
    get() {
        return when (val back = background) {
            is ColorDrawable -> back.color
            is ColorStateList -> (back.current as ColorDrawable).color
            else -> 0
        }
    }

fun View.vibrate() {
    performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}

fun View.visibleIf(predicate: Boolean, default: Int = View.GONE) {
    visibility = if (predicate) {
        View.VISIBLE
    } else {
        default
    }
}

fun Activity.dismissKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (inputMethodManager.isActive) {
        this.currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}

fun Fragment.createCompose(resId: Int, content: @Composable () -> Unit): View {
    return ComposeView(requireContext()).apply {
        id = resId
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContent(content)
    }
}
