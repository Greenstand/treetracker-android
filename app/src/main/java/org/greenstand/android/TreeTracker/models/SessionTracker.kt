package org.greenstand.android.TreeTracker.models

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.user.User

class SessionTracker(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val users: Users,
) {

    var currentSessionId: Long? = null
    var currentUser: User? = null

    private var treesPlanted = 0

    suspend fun startSession(planterInfoId: Long) {
        endSession()

        withContext(Dispatchers.IO) {
            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val planterInfo = dao.getPlanterInfoById(planterInfoId) ?: throw IllegalStateException("Could not find planter info of id $planterInfoId")

            val sessionEntity = SessionEntity(
                uuid = UUID.randomUUID().toString(),
                planterInfoId = planterInfoId,
                wallet = planterInfo.identifier,
                startTime = time,
                isUploaded = false,
                endTime = null,
                totalPlanted = 0,
                organization = planterInfo.organization, // TODO change to use current device org
                plantedWithConnection = 0,
            )

            currentSessionId = dao.insertSession(sessionEntity)

            currentUser = users.getUser(planterInfoId)
        }
    }

    suspend fun endSession() {
        currentSessionId?.let { id ->
            val session = dao.getSessionById(id).apply {
                totalPlanted = treesPlanted
                endTime = System.currentTimeMillis()
            }
            dao.updateSession(session)
            treesPlanted = 0
            currentUser = null
            currentSessionId = null
        }
    }

    fun treePlanted() {
        treesPlanted++
    }

}