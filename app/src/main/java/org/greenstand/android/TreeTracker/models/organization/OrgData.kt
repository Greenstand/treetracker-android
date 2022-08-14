package org.greenstand.android.TreeTracker.models.organization


data class Org(
    val id: String,
    val name: String,
    val walletId: String,
    val logoPath: String,
    val captureSetupFlow: List<Destination>,
)


data class Destination(
    val route: String,
    val features: List<String>? = null,
)