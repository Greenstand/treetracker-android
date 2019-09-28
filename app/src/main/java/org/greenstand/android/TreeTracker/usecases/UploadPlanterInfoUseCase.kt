package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber


data class UploadPlanterInfoParams(val planterInfoIds: List<Long>)

class UploadPlanterInfoUseCase(private val dao: TreeTrackerDAO,
                               private val objectStorageClient: ObjectStorageClient)
    : UseCase<UploadPlanterInfoParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterInfoUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterInfoParams) {

        val planterInfoList = params.planterInfoIds.mapNotNull { dao.getPlanterInfoById(it) }

        val registrationRequests = planterInfoList.map {
            RegistrationRequest(
                planterIdentifier = it.identifier,
                firstName = it.firstName,
                lastName = it.lastName,
                organization = it.organization,
                lat = it.latitude,
                long = it.longitude
            )
        }

        val jsonBundle = Gson().toJson(UploadBundle(registrations = registrationRequests))

        log("Creating MD5 hash")
        // Create a hash ID to reference this upload bundle later
        val bundleId = jsonBundle.md5()

        val planterInfoIds = planterInfoList.map { it.id }

        log("Updating UserInfo DB entries with MD5 hash")
        // Update the trees in DB with the bundleId
        dao.updatePlanterInfoBundleIds(planterInfoIds, bundleId)

        log("Uploading Bundle...")
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        log("Bundle Upload Completed")

        log("Updating UserInfo DB status to uploaded = true")
        dao.updatePlanterInfoUploadStatus(planterInfoIds, true)
    }

}