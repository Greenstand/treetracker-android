/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.appfunctions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utils.LocationUtils
import java.util.UUID

class AgentTreeCapturer(
    private val context: Context,
    private val dao: TreeTrackerDAO,
    private val userRepo: UserRepo,
    private val sessionTracker: SessionTracker,
    private val deviceConfigUpdater: DeviceConfigUpdater,
    private val orgRepo: OrgRepo,
    private val createTreeUseCase: CreateTreeUseCase,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val locationUpdateManager: LocationUpdateManager,
    private val locationManager: LocationManager,
) {
    suspend fun captureTrees(
        count: Int,
        wallet: String?,
        latitude: Double?,
        longitude: Double?,
        note: String?,
    ): AgentCapturedTrees {
        AgentInput.requireTreeCount(count)
        val center = AgentInput.treeCenter(latitude, longitude) ?: deviceLocation()

        val activeSessionId = sessionTracker.currentSessionId
        if (activeSessionId != null) {
            val sessionWallet = dao.getSessionById(activeSessionId).originWallet
            require(wallet == null || wallet.trim().equals(sessionWallet, ignoreCase = true)) {
                "A capture session for '$sessionWallet' is in progress. Omit wallet to add trees to it, " +
                    "or leave the capture flow first."
            }
            val treeUuids = saveTrees(activeSessionId, count, center, note)
            treesToSyncHelper.refreshTreeCountToSync()
            return AgentCapturedTrees(activeSessionId, sessionWallet, treeUuids.size, treeUuids)
        }

        val user =
            if (wallet != null) {
                requireNotNull(AgentInput.walletLookups(wallet).firstNotNullOfOrNull { userRepo.getUserWithWallet(it) }) {
                    "No account with wallet '$wallet'. Call createAccount first."
                }
            } else {
                requireNotNull(userRepo.getPowerUser()) { "No account exists yet. Call createAccount first." }
            }

        deviceConfigUpdater.saveLatestConfig()
        orgRepo.ensureInitialized()

        val sessionId =
            withContext(NonCancellable) {
                sessionTracker.createSession(userId = user.id, organizationName = null, sessionNote = null).id
            }
        val treeUuids =
            try {
                saveTrees(sessionId, count, center, note)
            } finally {
                withContext(NonCancellable) {
                    sessionTracker.closeSession(sessionId)
                    treesToSyncHelper.refreshTreeCountToSync()
                }
            }
        return AgentCapturedTrees(sessionId, user.wallet, treeUuids.size, treeUuids)
    }

    private suspend fun saveTrees(
        sessionId: Long,
        count: Int,
        center: Pair<Double, Double>?,
        note: String?,
    ): List<String> =
        List(count) {
            val (treeLatitude, treeLongitude) = center?.let { spreadAround(it) } ?: (0.0 to 0.0)
            val tree =
                Tree(
                    treeUuid = UUID.randomUUID(),
                    sessionId = sessionId,
                    content = note.orEmpty(),
                    photoPath = ImageUtils.createTestImageFile(context).absolutePath,
                    meanLongitude = treeLongitude,
                    meanLatitude = treeLatitude,
                )
            tree.addTreeAttribute(Tree.ABS_STEP_COUNT_KEY, "0")
            tree.addTreeAttribute(Tree.DELTA_STEP_COUNT_KEY, "0")
            createTreeUseCase.execute(tree)
            tree.treeUuid.toString()
        }

    private fun spreadAround(center: Pair<Double, Double>): Pair<Double, Double> {
        val (latitude, longitude) = LocationUtils.generateRandomLocationInRadius(center.first, center.second, TREE_SPREAD_MILES)
        return AgentInput.normalizeCoordinates(latitude, longitude)
    }

    private fun deviceLocation(): Pair<Double, Double>? {
        val location = locationUpdateManager.currentLocation ?: lastKnownLocation()
        return location?.let { it.latitude to it.longitude }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null
        return locationManager
            .getProviders(true)
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
    }

    companion object {
        private const val TREE_SPREAD_MILES = 0.05
    }
}