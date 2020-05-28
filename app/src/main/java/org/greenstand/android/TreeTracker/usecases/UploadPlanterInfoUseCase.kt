package org.greenstand.android.TreeTracker.usecases

import com.google.gson.Gson
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber


data class UploadPlanterInfoParams(val planterInfoIds: List<Long>)

class UploadPlanterInfoUseCase(private val dao: TreeTrackerDAO,
                               private val objectStorageClient: ObjectStorageClient)
    : UseCase<UploadPlanterInfoParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterInfoUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterInfoParams) {

        val planterInfoList = params.planterInfoIds
            .mapNotNull { dao.getPlanterInfoById(it) }
            .filterNot { it.uploaded }

        if (planterInfoList.isEmpty()) {
            log("All PlanterInfo uploaded, skipping PlanterInfo upload.")
            return
        }

        val registrationRequests = planterInfoList.map {
            RegistrationRequest(
                planterIdentifier = it.identifier,
                firstName = it.firstName,
                lastName = it.lastName,
                organization = it.organization,
                phone = it.phone,
                email = it.email,
                lat = it.latitude,
                lon = it.longitude,
                deviceIdentifier = DeviceUtils.deviceId,
                recordUuid = it.recordUuid
            )
        }

        val jsonBundle = Gson().toJson(UploadBundle(registrations = registrationRequests))

        // Create a hash ID to reference this upload bundle later
        val bundleId = jsonBundle.md5()

        val planterInfoIds = planterInfoList.map { it.id }

        // Update the trees in DB with the bundleId
        dao.updatePlanterInfoBundleIds(planterInfoIds, bundleId)

        log("Uploading UserInfo Bundle...")
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        log("Bundle UserInfo Upload Completed")

        dao.updatePlanterInfoUploadStatus(planterInfoIds, true)
    }

}