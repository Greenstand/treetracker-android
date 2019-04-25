package org.greenstand.android.TreeTracker.utilities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions

object CameraHelper {

    fun takePictureForResult(fragment: Fragment) {
        if (ActivityCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                        Permissions.MY_PERMISSION_CAMERA)

        } else {
            val takePictureIntent = Intent(fragment.requireContext(), CameraActivity::class.java)
            takePictureIntent.putExtra(ValueHelper.TAKE_SELFIE_EXTRA, true)
            fragment.startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA)
        }
    }

}