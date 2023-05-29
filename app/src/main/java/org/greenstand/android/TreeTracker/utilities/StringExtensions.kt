/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.utilities

import java.security.MessageDigest
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

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hashString(algorithmName: String): String {
    val bytes = MessageDigest
        .getInstance(algorithmName)
        .digest(this.toByteArray())
    val result = StringBuilder(bytes.size * 2)
    bytes.forEach {
        val i = it.toInt()
        result.append(HEX_CHARS[(i shr 4) and 0x0f])
        result.append(HEX_CHARS[i and 0x0f])
    }
    return result.toString()
}