package org.greenstand.android.TreeTracker.models.setupflow

import org.greenstand.android.TreeTracker.models.user.User

data class CaptureSetupData(
    var user: User? = null,
    var destinationWallet: String? = null,
    var sessionNote: String? = null,
    var organizationName: String? = null,
)