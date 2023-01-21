package org.greenstand.android.TreeTracker.models.organization

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

// TEST DEEPLINK: app://mobile.treetracker.org/org?params={"id":"109288091","version":"1","name":"Kasiki Hai","walletId":"klasdlk1-a0a23lmnzcln9o3","captureSetupFlow":[{"route":"user-select"},{"route":"session-note"}],"captureFlow":[{"route":"capture/{profilePicUrl}"},{"route":"tree-image-review/{photoPath}"},{"route":"tree-height-selection"}]}

class OrgRepo(
    private val dao: TreeTrackerDAO,
    private val prefs: Preferences,
    private val gson: Gson,
) {

    private var currentOrg: Org? = null

    suspend fun init() {
        dao.insertOrg(
            OrganizationEntity(
                id = "-1",
                version = 1,
                name = "Greenstand",
                walletId = "",
                captureSetupFlowJson = gson.toJson(listOf(
                    Destination(NavRoute.UserSelect.route),
                    Destination(NavRoute.WalletSelect.route),
                    Destination(NavRoute.AddOrg.route),
                )),
                captureFlowJson = gson.toJson(listOf(
                    Destination(NavRoute.TreeCapture.route),
                    Destination(NavRoute.TreeImageReview.route),
                    // For Kasiki Hai
//                    Destination(NavRoute.TreeHeightScreen.route),
                )),
            )
        )
        val currentOrgId = prefs.getString(CURRENT_ORG_ID_KEY, "-1")
        currentOrg = dao.getOrg(currentOrgId)?.toOrg()
        Timber.tag("OrgRepo").d("Org set to: ${currentOrg!!.name}")
        Timber.tag("OrgRepo").d("Org settings: $currentOrg")
    }

    suspend fun getOrgs(): List<Org> {
        return dao.getAllOrgs().map { it.toOrg() }
    }

    suspend fun setOrg(orgId: String) {
        prefs.edit().putString(CURRENT_ORG_ID_KEY, orgId).commit()
        currentOrg = dao.getOrg(orgId)?.toOrg()
        Timber.tag("OrgRepo").d("Org set to: ${currentOrg!!.name}")
        Timber.tag("OrgRepo").d("Org settings: $currentOrg")
    }

    fun currentOrg(): Org = currentOrg!!

    suspend fun addOrgFromJsonString(orgJsonString: String) {
        val orgJsonObj = JsonParser().parse(orgJsonString).asJsonObject
        val orgEntity = OrganizationEntity(
            id = orgJsonObj.get(OrgJsonKeys.V1.ID).asString,
            version = orgJsonObj.get(OrgJsonKeys.V1.VERSION).asInt,
            name = orgJsonObj.get(OrgJsonKeys.V1.NAME).asString,
            walletId = orgJsonObj.get(OrgJsonKeys.V1.WALLET_ID).asString,
            captureSetupFlowJson = orgJsonObj.get(OrgJsonKeys.V1.CAPTURE_SETUP_FLOW).asJsonArray.toString(),
            captureFlowJson = orgJsonObj.get(OrgJsonKeys.V1.CAPTURE_FLOW).asJsonArray.toString(),
        )
        dao.insertOrg(orgEntity)
        setOrg(orgEntity.id)
    }

    private fun OrganizationEntity.toOrg(): Org {
        val typeToken = object : TypeToken<List<Destination>>(){}.type
        val captureSetupDestinations = gson.fromJson<List<Destination>>(captureSetupFlowJson, typeToken)
        val captureDestinations = gson.fromJson<List<Destination>>(captureFlowJson, typeToken)
        return Org(
            id = id,
            name = name,
            walletId = walletId,
            logoPath = "",
            captureSetupFlow = captureSetupDestinations,
            captureFlow = captureDestinations
        )
    }

    companion object {
        private val CURRENT_ORG_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}