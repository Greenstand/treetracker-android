package org.greenstand.android.TreeTracker.models.organization

import com.google.gson.JsonParser
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

// TEST DEEPLINK: app://mobile.treetracker.org/org?params={"id":"123abc","version": "1","name": "kasiki hai","walletId": "123abc","isTokenXferChoiceEnabled": false,"isSessionNoteEnabled": true}

class OrgRepo(
    private val dao: TreeTrackerDAO,
    private val prefs: Preferences,
) {

    private var currentOrg: Org? = null

    suspend fun init() {
        dao.insertOrg(
            OrganizationEntity(
                id = "-1",
                version = 1,
                name = "Greenstand",
                walletId = "",
                isTokenTransferChoiceEnabled = true,
                isSessionNoteEnabled = false,
            )
        )
        val currentOrgId = prefs.getString(CURRENT_ORG_ID_KEY, "-1")
        currentOrg = dao.getOrg(currentOrgId)?.toOrg()
    }

    suspend fun getOrgs(): List<Org> {
        return dao.getAllOrgs().map { it.toOrg() }
    }

    suspend fun setOrg(orgId: String) {
        prefs.edit().putString(CURRENT_ORG_ID_KEY, orgId).commit()
        currentOrg = dao.getOrg(orgId)?.toOrg()
    }

    fun currentOrg(): Org = currentOrg!!

    suspend fun addOrgFromJsonString(orgJsonString: String) {
        val orgJsonObj = JsonParser().parse(orgJsonString).asJsonObject
        val orgEntity = OrganizationEntity(
            id = orgJsonObj.get(OrgJsonKeys.V1.ID).asString,
            version = orgJsonObj.get(OrgJsonKeys.V1.VERSION).asInt,
            name = orgJsonObj.get(OrgJsonKeys.V1.NAME).asString,
            walletId = orgJsonObj.get(OrgJsonKeys.V1.WALLET_ID).asString,
            isTokenTransferChoiceEnabled = orgJsonObj.get(OrgJsonKeys.V1.IS_TOKEN_XFER_CHOICE_ENABLED).asBoolean,
            isSessionNoteEnabled = orgJsonObj.get(OrgJsonKeys.V1.IS_SESSION_NOTE_ENABLED).asBoolean,
        )
        dao.insertOrg(orgEntity)
    }

    private fun OrganizationEntity.toOrg(): Org {
        return Org(
            id = id,
            name = name,
            walletId = walletId,
            logoPath = "",
            isTokenTransferChoiceEnabled = isTokenTransferChoiceEnabled,
            isSessionNoteEnabled = isSessionNoteEnabled,
        )
    }

    companion object {
        private val CURRENT_ORG_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}