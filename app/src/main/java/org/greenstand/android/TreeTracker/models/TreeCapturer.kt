package org.greenstand.android.TreeTracker.models

import java.io.File
import java.util.UUID
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase

class TreeCapturer(
    private val locationDataCapturer: LocationDataCapturer,
    private val stepCounter: StepCounter,
    private val createTreeUseCase: CreateTreeUseCase,
    private val deviceOrientation: DeviceOrientation,
    private val users: Users,
) {

    private var newTreeUuid: UUID? = null
    private var convergence: Convergence? = null
    private var currentTree: Tree? = null

    suspend fun pinLocation(): Boolean {
        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()
        locationDataCapturer.converge()

        newTreeUuid = locationDataCapturer.generatedTreeUuid
        return if (locationDataCapturer.lastConvergenceWithinRange != null || locationDataCapturer.currentConvergence != null) {
            convergence = locationDataCapturer.convergence()
            locationDataCapturer.turnOffTreeCaptureMode()
            true
        } else {
            locationDataCapturer.turnOffTreeCaptureMode()
            false
        }
    }

    suspend fun setImage(imageFile: File) {
        val tree = Tree(
            treeUuid = newTreeUuid!!,
            planterCheckInId = users.currentSessionUser?.id ?: -1,
            content = "",
            photoPath = imageFile.absolutePath,
            convergence?.longitudeConvergence?.mean ?: 0.0,
            convergence?.latitudeConvergence?.mean ?: 0.0
        )
        tree.addTreeAttribute(Tree.ABS_STEP_COUNT_KEY, (stepCounter.absoluteStepCount ?: 0).toString())
        tree.addTreeAttribute(Tree.DELTA_STEP_COUNT_KEY, stepCounter.deltaSteps.toString())
        deviceOrientation.rotationMatrixSnapshot?.let {
            tree.addTreeAttribute(
                Tree.ROTATION_MATRIX_KEY, it.joinToString(",")
            )
        }
        currentTree = tree

        stepCounter.snapshotAbsoluteStepCountOnTreeCapture()
    }

    fun setNote(note: String) {
        currentTree = currentTree?.copy(content = note)
    }

    suspend fun saveTree() {
        currentTree?.let {
            createTreeUseCase.execute(it)
        }
    }


}