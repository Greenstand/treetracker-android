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

// TEST DEEPLINK: app://mobile.treetracker.org/org?id=109288091&name=Kasiki%20Hai
// TEST DEEPLINK: app://mobile.treetracker.org/org?params={"id":"109288091","version":"1","name":"Kasiki Hai","walletId":"klasdlk1-a0a23lmnzcln9o3","captureSetupFlow":[{"route":"user-select"},{"route":"session-note"}],"captureFlow":[{"route":"capture/{profilePicUrl}"},{"route":"tree-image-review/{photoPath}"},{"route":"tree-height-selection"}]}

class OrgRepo(
    private val dao: TreeTrackerDAO,
    private val prefs: Preferences,
    private val json: Json,
) {
    private var currentOrg: Org? = null

    private val ORG_CONFIG_SYNCED_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("org-config-synced")

    // In OrgRepo.kt
    fun hasCompletedInitialOrgSync(): Boolean =
        prefs.getBoolean(ORG_CONFIG_SYNCED_KEY, false)

    fun markInitialOrgSyncComplete() {
        prefs.edit().putBoolean(ORG_CONFIG_SYNCED_KEY, true).commit()
    }

    fun resetInitialOrgSync() {
        prefs.edit().putBoolean(ORG_CONFIG_SYNCED_KEY, false).commit()
    }

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

    suspend fun addOrgFromRemoteConfig(
        orgId: String,
        orgName: String,
        configJson: String,
    ): Boolean =
        try {
            Timber.tag(ORG_LINK_TAG).d("Parsing Remote Config for org $orgId ('$orgName')")
            val configObj = Json.parseToJsonElement(configJson).jsonObject
            val walletId = configObj[OrgJsonKeys.V1.WALLET_ID]?.jsonPrimitive?.content ?: ""
            val setupFlowJson = configObj[OrgJsonKeys.V1.CAPTURE_SETUP_FLOW]?.jsonArray
            val captureFlowJson = configObj[OrgJsonKeys.V1.CAPTURE_FLOW]?.jsonArray
            Timber.tag(ORG_LINK_TAG).d(
                "Parsed config: walletId=$walletId, setupFlow=${setupFlowJson?.size ?: 0} steps, captureFlow=${captureFlowJson?.size ?: 0} steps",
            )
            val orgEntity =
                OrganizationEntity(
                    id = orgId,
                    version = configObj[OrgJsonKeys.V1.VERSION]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                    name = orgName,
                    walletId = walletId,
                    captureSetupFlowJson = setupFlowJson.toString(),
                    captureFlowJson = captureFlowJson.toString(),
                )
            val validatedEntity = validateOrgRoutes(orgEntity)
            dao.insertOrg(validatedEntity)
            setOrg(validatedEntity.id)
            Timber.tag(ORG_LINK_TAG).i("Org '$orgName' ($orgId) loaded from Remote Config")
            resetInitialOrgSync()
            Timber.tag(ORG_LINK_TAG).i("Org config sync reset")
            addMinimalOrg(orgId, orgName)
            Timber.tag(ORG_LINK_TAG).i("Minimal org '$orgName' ($orgId) added with default flows")
            true
        } catch (e: Exception) {
            Timber.tag(ORG_LINK_TAG).e(e, "Failed to parse Remote Config for org $orgId, falling back to minimal org")
            addMinimalOrg(orgId, orgName)
        }

    suspend fun addMinimalOrg(
        orgId: String,
        orgName: String,
    ): Boolean =
        try {
            Timber.tag(ORG_LINK_TAG).d("Creating minimal org: id=$orgId, name='$orgName'")
            val orgEntity =
                OrganizationEntity(
                    id = orgId,
                    version = 1,
                    name = orgName,
                    walletId = "",
                    captureSetupFlowJson =
                        json.encodeToString(
                            listOf(
                                Destination(RouteRegistry.ROUTE_USER_SELECT),
                            ),
                        ),
                    captureFlowJson =
                        json.encodeToString(
                            listOf(
                                Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                                Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                            ),
                        ),
                )
            dao.insertOrg(orgEntity)
            setOrg(orgEntity.id)
            Timber.tag(ORG_LINK_TAG).i("Minimal org '$orgName' ($orgId) created with default flows")
            true
        } catch (e: Exception) {
            Timber.tag(ORG_LINK_TAG).e(e, "Failed to create minimal org for $orgId")
            false
        }

    private fun validateOrgRoutes(entity: OrganizationEntity): OrganizationEntity {
        val setupFlow = json.decodeFromString<List<Destination>>(entity.captureSetupFlowJson)
        val captureFlow = json.decodeFromString<List<Destination>>(entity.captureFlowJson)
        val invalidSetup = setupFlow.filter { !RouteRegistry.isValidRoute(it.route) }
        val invalidCapture = captureFlow.filter { !RouteRegistry.isValidRoute(it.route) }
        if (invalidSetup.isNotEmpty()) {
            Timber.tag(ORG_LINK_TAG).w("Dropping unknown setup flow routes: ${invalidSetup.map { it.route }}")
        }
        if (invalidCapture.isNotEmpty()) {
            Timber.tag(ORG_LINK_TAG).w("Dropping unknown capture flow routes: ${invalidCapture.map { it.route }}")
        }
        val validSetup = setupFlow.filter { RouteRegistry.isValidRoute(it.route) }
        val validCapture = captureFlow.filter { RouteRegistry.isValidRoute(it.route) }
        Timber.tag(ORG_LINK_TAG).d(
            "Validated routes: setupFlow=${validSetup.map { it.route }}, captureFlow=${validCapture.map { it.route }}",
        )
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
        private const val ORG_LINK_TAG = "OrgLink"
        private val CURRENT_ORG_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}