package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName
import org.greenstand.android.TreeTracker.BuildConfig

/**
 * Created by zaven on 4/8/18.
 */

data class AuthenticationRequest(@SerializedName("client_id")
                                 val clientId: String = BuildConfig.TREETRACKER_CLIENT_ID,
                                 @SerializedName("client_secret")
                                 val clientSecret: String = BuildConfig.TREETRACKER_CLIENT_SECRET,
                                 @SerializedName("device_android_id")
                                 val deviceAndroidId: String)
