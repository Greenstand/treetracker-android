package org.greenstand.android.TreeTracker.utilities

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.greenstand.android.TreeTracker.activities.ImageCaptureActivity
import org.greenstand.android.TreeTracker.application.Permissions

object CameraHelper {

    private const val CAMERA_INTENT = 1001

    fun takePictureForResult(fragment: Fragment, selfie: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                    fragment.requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            fragment.requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Permissions.MY_PERMISSION_CAMERA
            )
        } else {
            val intent = ImageCaptureActivity.createIntent(fragment.requireContext(), selfie)
            fragment.startActivityForResult(intent, CAMERA_INTENT)
        }
    }

    fun wasCameraPermissionGranted(requestCode: Int, grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() &&
            requestCode == Permissions.MY_PERMISSION_CAMERA &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
    }
}
