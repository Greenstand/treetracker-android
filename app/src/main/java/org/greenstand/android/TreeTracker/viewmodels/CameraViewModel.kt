package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.ViewModel
import kotlin.properties.Delegates

class CameraViewModel : ViewModel() {

    var isFrontFacing: Boolean by Delegates.notNull()

    fun startCamera() {

    }


}