package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity

data class UploadPlanterDetailsParams(val planterDetailsId: Int)

class UploadPlanterDetailsUseCase(private val db: AppDatabase,
                                  private val api: RetrofitApi,
                                  private val uploadImageUseCase: UploadImageUseCase) : UseCase<UploadPlanterDetailsParams, Unit>() {

    override suspend fun execute(params: UploadPlanterDetailsParams) {

        val planterDetailsEntity = db.planterDao().getPlanterDetailsById(params.planterDetailsId)

        if (planterDetailsEntity == null) {
            throw IllegalStateException("No planter details with id: ${params.planterDetailsId} found")
        }

        val registration = RegistrationRequest(
            planterIdentifier = planterDetailsEntity.identifier,
            firstName = planterDetailsEntity.firstName,
            lastName = planterDetailsEntity.lastName,
            organization = planterDetailsEntity.organization,
            location = planterDetailsEntity.location
        )

        api.createPlanterRegistration(registration)

        planterDetailsEntity.uploaded = true
        db.planterDao().updatePlanterDetails(planterDetailsEntity)

        val planterDetailsList: List<PlanterIdentificationsEntity> = db.planterDao().getPlanterIdentificationsToUpload()

        planterDetailsList.forEach { planterIndentification ->
            val imageUrl = withContext(Dispatchers.IO) { uploadImageUseCase.execute(UploadImageParams(planterIndentification.photoPath!!)) }
            planterIndentification.photoUrl = imageUrl
            db.planterDao().updatePlanterIdentification(planterIndentification)
        }
    }

}