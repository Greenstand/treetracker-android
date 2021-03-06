package org.greenstand.android.TreeTracker.models

import androidx.compose.material.Colors
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

// TODO finish setup of Org data with a db table to back it up
data class Org(
    val name: String,
    val walletId: String,
    val themeColors: Colors,
    val logoUrl: String,
    val captureInputs: List<String>
)

class Organizations(
    private val dao: TreeTrackerDAO
) {

    var currentOrg: Org? = null
        private set

    suspend fun getOrgs(): List<Org> {
        TODO("Return a list of saved organizations on device")
    }

    suspend fun addOrg(org: Org) {
        TODO("Save org to db")
    }

}