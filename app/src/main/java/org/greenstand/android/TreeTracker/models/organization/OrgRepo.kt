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
package org.greenstand.android.TreeTracker.models.organization

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
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
        dao.insertOrg(defaultOrgEntity())
        val currentOrgId = prefs.getString(CURRENT_ORG_ID_KEY, DEFAULT_ORG_ID)
        currentOrg = dao.getOrg(currentOrgId)?.toOrg()
        if (currentOrg == null) {
            Timber.tag("OrgRepo").w("Stored org ID '$currentOrgId' not found, falling back to default")
            currentOrg = dao.getOrg(DEFAULT_ORG_ID)?.toOrg()
        }
        Timber.tag("OrgRepo").d("Org set to: ${currentOrg?.name}")
        Timber.tag("OrgRepo").d("Org settings: $currentOrg")
    }

    private fun defaultOrgEntity() = OrganizationEntity(
        id = DEFAULT_ORG_ID,
        version = 1,
        name = "Greenstand",
        walletId = "",
        captureSetupFlowJson = gson.toJson(
            listOf(
                Destination(RouteRegistry.ROUTE_USER_SELECT),
//              Destination(RouteRegistry.ROUTE_WALLET_SELECT),
                Destination(RouteRegistry.ROUTE_ADD_ORG),
            )
        ),
        captureFlowJson = gson.toJson(
            listOf(
                Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                // Uncomment this to test out forcing the note taking feature
//              Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                // For Kasiki Hai
//              Destination(RouteRegistry.ROUTE_TREE_HEIGHT),
            )
        ),
    )

    suspend fun getOrgs(): List<Org> {
        return dao.getAllOrgs().map { it.toOrg() }
    }

    suspend fun setOrg(orgId: String) {
        prefs.edit().putString(CURRENT_ORG_ID_KEY, orgId).commit()
        currentOrg = dao.getOrg(orgId)?.toOrg()
        if (currentOrg == null) {
            Timber.tag("OrgRepo").w("Org '$orgId' not found after setOrg, falling back to default")
            currentOrg = dao.getOrg(DEFAULT_ORG_ID)?.toOrg()
        }
        Timber.tag("OrgRepo").d("Org set to: ${currentOrg?.name}")
        Timber.tag("OrgRepo").d("Org settings: $currentOrg")
    }

    fun currentOrg(): Org {
        return currentOrg ?: error(
            "OrgRepo not initialized. Call init() before accessing currentOrg()."
        )
    }

    suspend fun addOrgFromJsonString(orgJsonString: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val orgJsonObj = JsonParser().parse(orgJsonString).asJsonObject
            val orgEntity = OrganizationEntity(
                id = orgJsonObj.get(OrgJsonKeys.V1.ID).asString,
                version = orgJsonObj.get(OrgJsonKeys.V1.VERSION).asInt,
                name = orgJsonObj.get(OrgJsonKeys.V1.NAME).asString,
                walletId = orgJsonObj.get(OrgJsonKeys.V1.WALLET_ID).asString,
                captureSetupFlowJson = orgJsonObj.get(OrgJsonKeys.V1.CAPTURE_SETUP_FLOW).asJsonArray.toString(),
                captureFlowJson = orgJsonObj.get(OrgJsonKeys.V1.CAPTURE_FLOW).asJsonArray.toString(),
            )
            val validatedEntity = validateOrgRoutes(orgEntity)
            dao.insertOrg(validatedEntity)
            setOrg(validatedEntity.id)
            true
        } catch (e: Exception) {
            Timber.tag("OrgRepo").e(e, "Failed to parse org JSON, falling back to default org")
            false
        }
    }

    private fun validateOrgRoutes(entity: OrganizationEntity): OrganizationEntity {
        val typeToken = object : TypeToken<List<Destination>>() {}.type
        val setupFlow = gson.fromJson<List<Destination>>(entity.captureSetupFlowJson, typeToken)
        val captureFlow = gson.fromJson<List<Destination>>(entity.captureFlowJson, typeToken)
        val invalidSetup = setupFlow.filter { !RouteRegistry.isValidRoute(it.route) }
        val invalidCapture = captureFlow.filter { !RouteRegistry.isValidRoute(it.route) }
        if (invalidSetup.isNotEmpty()) {
            Timber.tag("OrgRepo").w("Unknown setup flow routes: ${invalidSetup.map { it.route }}")
        }
        if (invalidCapture.isNotEmpty()) {
            Timber.tag("OrgRepo").w("Unknown capture flow routes: ${invalidCapture.map { it.route }}")
        }
        val validSetup = setupFlow.filter { RouteRegistry.isValidRoute(it.route) }
        val validCapture = captureFlow.filter { RouteRegistry.isValidRoute(it.route) }
        return entity.copy(
            captureSetupFlowJson = gson.toJson(validSetup),
            captureFlowJson = gson.toJson(validCapture),
        )
    }

    private fun OrganizationEntity.toOrg(): Org {
        val typeToken = object : TypeToken<List<Destination>>() {}.type
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
        const val DEFAULT_ORG_ID = "-1"
        private val CURRENT_ORG_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}