package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.text.Spanned
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat

object TextUtils {

    fun createColorizedText(text: String, context: Context, @ColorRes colorRes: Int): Spanned {
        return HtmlCompat.fromHtml("<font color=\"" + ContextCompat.getColor(context, colorRes) + "\">" + "Stop" + "</font>", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
