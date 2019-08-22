package org.greenstand.android.TreeTracker.utilities

import java.security.NoSuchAlgorithmException


fun String.md5(): String {

    try {
        // Create MD5 Hash
        val digest = java.security.MessageDigest.getInstance("MD5")
        digest.update(this.toByteArray())
        val messageDigest = digest.digest()

        val MD5Hash = StringBuffer()
        for (i in messageDigest.indices) {
            var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
            while (h.length < 2)
                h = "0$h"
            MD5Hash.append(h)
        }

        return MD5Hash.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return ""

}