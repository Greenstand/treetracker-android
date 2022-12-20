package org.greenstand.android.TreeTracker.models

import com.google.gson.Gson
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.DeviceConfigRequest
import org.greenstand.android.TreeTracker.api.models.requests.SessionRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5

class SessionUploader(
    private val dao: TreeTrackerDAO,
    private val objectStorageClient: ObjectStorageClient,
    private val gson: Gson,
) {

    suspend fun upload() {
        val sessionsToUpload = dao.getSessionsToUpload()

        val sessionRequests = sessionsToUpload.map { session ->
            SessionRequest(
                sessionId = session.uuid,
                originUserId = session.originUserId,
                targetWallet = session.destinationWallet,
                organization = session.organization ?: "",
                deviceConfigId = dao.getDeviceConfigById(session.deviceConfigId!!)!!.uuid,
                startTime = session.startTime.toEpochMilliseconds()
            )
        }

        val jsonBundle = gson.toJson(UploadBundle.createV2(
            sessions = sessionRequests,
        ))
        val bundleId = jsonBundle.md5() + "_sessions"
        val sessionIds = sessionsToUpload.map { it.id }

        // Update the trees in DB with the bundleId
        dao.updateSessionBundleIds(sessionIds, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        dao.updateSessionUploadStatus(sessionIds, true)
    }
}