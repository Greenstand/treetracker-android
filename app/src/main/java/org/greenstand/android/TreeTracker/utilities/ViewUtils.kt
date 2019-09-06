package org.greenstand.android.TreeTracker.utilities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.HapticFeedbackConstants
import android.view.View
import com.google.android.material.card.MaterialCardView


fun View.animateColor(toColor: Int, fromColor: Int = color, durationMsec: Long = 300) {
    ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
        duration = durationMsec
        addUpdateListener { animator -> setBackgroundColor(animator.animatedValue as Int) }
        start()
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

val MaterialCardView.cardColor: Int
    get() {
        return cardBackgroundColor.defaultColor
    }


fun View.vibrate() {
    performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}