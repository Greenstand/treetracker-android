package org.greenstand.android.TreeTracker.models

import androidx.compose.material.Colors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel

// TODO finish setup of Org data with a db table to back it up
data class Org(
    val name: String,
    val walletId: String,
    val themeColors: Colors,
    val logoUrl: String,
    val captureInputs: List<String>
)

class OrgPickerViewModelPreview : PreviewParameterProvider<OrgPickerViewModel> {
    override val values = sequenceOf(OrgPickerViewModel(OrganizationsFake()))
    override val count: Int = values.count()
}

interface Organizations {

    suspend fun getOrgs(): List<Org>

    suspend fun addOrg(org: Org)

    suspend fun getCurrentOrg(): Org?

    suspend fun setCurrentOrg(org: Org?)
}

class OrganizationsImpl(
    private val dao: TreeTrackerDAO
) : Organizations {

    var currentOrg: Org? = null
        private set

    override suspend fun getOrgs(): List<Org> {
        TODO("Return a list of saved organizations on device")
    }

    override suspend fun addOrg(org: Org) {
        TODO("Save org to db")
    }

    override suspend fun getCurrentOrg(): Org? {
        TODO("Not yet implemented")
    }

    override suspend fun setCurrentOrg(org: Org?) {
        currentOrg = org
        // TODO set org as current in db
    }
}

class OrganizationsFake : Organizations {

    private val themeOne = lightColors()
    private val themeTwo = lightColors(
        primary = Color.Yellow,
        secondary = Color.Blue
    )

    private val orgs = mutableListOf(
        Org(
            name = "Kasiki Hai",
            walletId = "@Kasiki.Hai",
            themeColors = themeOne,
            logoUrl = "",
            captureInputs = emptyList()
        ),
        Org(
            name = "Greenstand",
            walletId = "@Greenstand",
            themeColors = themeTwo,
            logoUrl = "",
            captureInputs = emptyList()
        ),
        Org(
            name = "GreenPlanet",
            walletId = "@GreenPlanet",
            themeColors = themeOne,
            logoUrl = "",
            captureInputs = emptyList()
        ),
        Org(
            name = "PlantersPro",
            walletId = "@PlantersPro",
            themeColors = themeTwo,
            logoUrl = "",
            captureInputs = emptyList()
        )
    )

    var currentOrg: Org? = null
        private set

    override suspend fun getOrgs(): List<Org> = orgs

    override suspend fun addOrg(org: Org) {
        orgs.add(org)
    }

    override suspend fun getCurrentOrg(): Org? = currentOrg

    override suspend fun setCurrentOrg(org: Org?) {
        currentOrg = org
    }
}
