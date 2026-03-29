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

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val json: Json,
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

    private fun defaultOrgEntity() =
        OrganizationEntity(
            id = DEFAULT_ORG_ID,
            version = 1,
            name = "Greenstand",
            walletId = "",
            captureSetupFlowJson =
                json.encodeToString(
                    listOf(
                        Destination(RouteRegistry.ROUTE_USER_SELECT),
//              Destination(RouteRegistry.ROUTE_WALLET_SELECT),
                        Destination(RouteRegistry.ROUTE_ADD_ORG),
                    ),
                ),
            captureFlowJson =
                json.encodeToString(
                    listOf(
                        Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                        // Uncomment this to test out forcing the note taking feature
//              Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                        Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                        // For Kasiki Hai
//              Destination(RouteRegistry.ROUTE_TREE_HEIGHT),
                    ),
                ),
        )

    suspend fun getOrgs(): List<Org> = dao.getAllOrgs().map { it.toOrg() }

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

    fun currentOrg(): Org =
        currentOrg ?: error(
            "OrgRepo not initialized. Call init() before accessing currentOrg().",
        )

    suspend fun addOrgFromJsonString(orgJsonString: String): Boolean =
        try {
            val orgJsonObj = Json.parseToJsonElement(orgJsonString).jsonObject
            val orgEntity =
                OrganizationEntity(
                    id = orgJsonObj[OrgJsonKeys.V1.ID]?.jsonPrimitive?.content ?: "",
                    version = orgJsonObj[OrgJsonKeys.V1.VERSION]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    name = orgJsonObj[OrgJsonKeys.V1.NAME]?.jsonPrimitive?.content ?: "",
                    walletId = orgJsonObj[OrgJsonKeys.V1.WALLET_ID]?.jsonPrimitive?.content ?: "",
                    captureSetupFlowJson = orgJsonObj[OrgJsonKeys.V1.CAPTURE_SETUP_FLOW]?.jsonArray.toString(),
                    captureFlowJson = orgJsonObj[OrgJsonKeys.V1.CAPTURE_FLOW]?.jsonArray.toString(),
                )
            val validatedEntity = validateOrgRoutes(orgEntity)
            dao.insertOrg(validatedEntity)
            setOrg(validatedEntity.id)
            true
        } catch (e: Exception) {
            Timber.tag("OrgRepo").e(e, "Failed to parse org JSON, falling back to default org")
            false
        }

    private fun validateOrgRoutes(entity: OrganizationEntity): OrganizationEntity {
        val setupFlow = json.decodeFromString<List<Destination>>(entity.captureSetupFlowJson)
        val captureFlow = json.decodeFromString<List<Destination>>(entity.captureFlowJson)
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
            captureSetupFlowJson = json.encodeToString(validSetup),
            captureFlowJson = json.encodeToString(validCapture),
        )
    }

    private fun OrganizationEntity.toOrg(): Org {
        val captureSetupDestinations = json.decodeFromString<List<Destination>>(captureSetupFlowJson)
        val captureDestinations = json.decodeFromString<List<Destination>>(captureFlowJson)
        return Org(
            id = id,
            name = name,
            walletId = walletId,
            logoPath = "",
            captureSetupFlow = captureSetupDestinations,
            captureFlow = captureDestinations,
        )
    }

    companion object {
        const val DEFAULT_ORG_ID = "-1"
        private val CURRENT_ORG_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}