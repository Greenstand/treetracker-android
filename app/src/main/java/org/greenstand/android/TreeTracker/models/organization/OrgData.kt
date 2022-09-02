package org.greenstand.android.TreeTracker.models.organization


data class Org(
    val id: String,
    val name: String,
    val walletId: String,
    val logoPath: String,
    // When false, the field transfer screen is disabled and planter tokens are given to the org wallet ID
    val isTokenTransferChoiceEnabled: Boolean,
    // When true, allows a note to be tied to the planting session
    val isSessionNoteEnabled: Boolean,
)