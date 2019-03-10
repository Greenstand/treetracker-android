package org.greenstand.android.TreeTracker.api.models.responses

/**
 * Created by zaven on 4/8/18.
 */

data class TokenResponse(val token: String? = null,
                         private val firstName: String? = null,
                         private val lastName: String? = null)
