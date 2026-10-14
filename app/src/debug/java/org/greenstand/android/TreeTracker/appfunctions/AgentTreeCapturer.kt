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

/**
 * Saves trees with the bundled test image, skipping the camera, GPS convergence and review
 * screens.
 *
 * With no capture session running, it saves the trees in a new session for the account and ends
 * it, as if the planter had tracked them and left the capture flow. That session never becomes
 * the app's current one, so a capture flow started meanwhile is unaffected. If a session is
 * already running (the app is in the capture flow), the trees are added to it and it is left open.
 *
 * Bad input throws [IllegalArgumentException] with a message meant for the calling agent.
 */
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
        require(count in 1..MAX_TREES) { "count must be between 1 and $MAX_TREES, was $count." }
        require((latitude == null) == (longitude == null)) { "Pass both latitude and longitude, or neither." }
        latitude?.let { require(it in -90.0..90.0) { "latitude must be between -90 and 90, was $it." } }
        longitude?.let { require(it in -180.0..180.0) { "longitude must be between -180 and 180, was $it." } }

        val center = captureCenter(latitude, longitude)

        val activeSessionId = sessionTracker.currentSessionId
        if (activeSessionId != null) {
            val sessionWallet = dao.getSessionById(activeSessionId).originWallet
            require(wallet == null || wallet.equals(sessionWallet, ignoreCase = true)) {
                "A capture session for '$sessionWallet' is in progress. Omit wallet to add trees to it, " +
                    "or leave the capture flow first."
            }
            val treeUuids = saveTrees(activeSessionId, count, center, note)
            treesToSyncHelper.refreshTreeCountToSync()
            return AgentCapturedTrees(activeSessionId, sessionWallet, treeUuids.size, treeUuids)
        }

        val user =
            if (wallet != null) {
                // Email wallets are saved lowercased
                requireNotNull(userRepo.getUserWithWallet(wallet) ?: userRepo.getUserWithWallet(wallet.lowercase())) {
                    "No account with wallet '$wallet'. Call createAccount first."
                }
            } else {
                requireNotNull(userRepo.getPowerUser()) { "No account exists yet. Call createAccount first." }
            }

        // The splash screen does both of these, but the app may never have been opened
        deviceConfigUpdater.saveLatestConfig()
        orgRepo.ensureInitialized()

        val sessionId = sessionTracker.createSession(userId = user.id, organizationName = null, sessionNote = null).id
        val treeUuids =
            try {
                saveTrees(sessionId, count, center, note)
            } finally {
                // Close the session even when the call is cancelled, or it stays open for good
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

    /**
     * A random point near [center]. Clamped and wrapped, because near a pole or the antimeridian
     * the spread can leave the valid range.
     */
    private fun spreadAround(center: Pair<Double, Double>): Pair<Double, Double> {
        val (latitude, longitude) = LocationUtils.generateRandomLocationInRadius(center.first, center.second, TREE_SPREAD_MILES)
        val wrappedLongitude = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return latitude.coerceIn(-90.0, 90.0) to wrappedLongitude
    }

    /** Latitude to longitude, or null when there is no location to use. */
    private fun captureCenter(
        latitude: Double?,
        longitude: Double?,
    ): Pair<Double, Double>? {
        if (latitude != null && longitude != null) return latitude to longitude
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
        const val MAX_TREES = 500

        // Trees land at random points within this radius of the center, so they don't stack on the map
        private const val TREE_SPREAD_MILES = 0.05
    }
}