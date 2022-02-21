package org.greenstand.android.TreeTracker.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.UUID
import org.greenstand.android.TreeTracker.activities.ImageCaptureActivity
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.models.location.Convergence
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.Planter
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase

class NewTreeViewModel(
    private val user: Planter,
    private val locationDataCapturer: LocationDataCapturer,
    private val createTreeUseCase: CreateTreeUseCase,
    private val analytics: Analytics,
    private val stepCounter: StepCounter,
    private val deviceOrientation: DeviceOrientation
) : ViewModel() {

    val onTreeSaved: MutableLiveData<Unit> = MutableLiveData()
    val navigateToTreeHeight: MutableLiveData<Tree> = MutableLiveData()
    val navigateBack: MutableLiveData<Unit> = MutableLiveData()
    val onTakePicture: MutableLiveData<Unit> = MutableLiveData()
    private var newTreeUuid: UUID? = null
    private var convergence: Convergence? = null

    val isNoteEnabled = FeatureFlags.TREE_NOTE_FEATURE_ENABLED

    val isDbhEnabled = FeatureFlags.TREE_DBH_FEATURE_ENABLED

    val isTreeHeightEnabled = FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED

    var isNextButtonActive = !isDbhEnabled
        private set

    var photoPath: String? = null

    init {
        if (photoPath == null) {
            onTakePicture.postValue(Unit)
        }
    }

    suspend fun createTree(note: String, dbh: String?) {

        val newTree = Tree(
            treeUuid = newTreeUuid!!,
            sessionId = user.planterCheckinId ?: -1,
            content = note,
            photoPath = photoPath!!,
            convergence?.longitudeConvergence?.mean ?: 0.0,
            convergence?.latitudeConvergence?.mean ?: 0.0
        )

        dbh?.let { newTree.addTreeAttribute(Tree.DBH_ATTR_KEY, it) }

        val absoluteStepCount = stepCounter.absoluteStepCount ?: 0
        val lastStepCountWhenCreatingTree = stepCounter.absoluteStepCountOnTreeCapture ?: 0
        // Delta step count is the difference between the absolute count at the time of capturing
        // a tree minus the last absolute step count recorded when capturing a previous tree. This
        // is the indicator for the number of steps taken between two trees.
        val deltaSteps = absoluteStepCount - lastStepCountWhenCreatingTree
        newTree.addTreeAttribute(Tree.ABS_STEP_COUNT_KEY, absoluteStepCount.toString())
        newTree.addTreeAttribute(Tree.DELTA_STEP_COUNT_KEY, deltaSteps.toString())
        deviceOrientation.rotationMatrixSnapshot?.let {
            newTree.addTreeAttribute(
                Tree.ROTATION_MATRIX_KEY, it.joinToString(",")
            )
        }
        if (newTree.content.isNotBlank()) {
            analytics.treeNoteAdded(newTree.content.length)
        }

        if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
            navigateToTreeHeight.postValue(newTree)
        } else {
            createTreeUseCase.execute(newTree)
            // Assign the current absolute step count to 'absoluteStepCountOnTreeCapture' to
            // enable step count delta calculation for the next tree capture
            stepCounter.snapshotAbsoluteStepCountOnTreeCapture()
            stepCounter.disable()
            onTreeSaved.postValue(Unit)
            navigateBack.postValue(Unit)
        }
    }

    fun isImageBlurry(data: Intent): Boolean {
        val imageQuality = data.getDoubleExtra(ImageCaptureActivity.FOCUS_METRIC_VALUE, 0.0)
        return imageQuality < FOCUS_THRESHOLD
    }

    suspend fun waitForConvergence() {
        stepCounter.enable()
        locationDataCapturer.turnOnTreeCaptureMode()
        locationDataCapturer.converge()
        newTreeUuid = locationDataCapturer.generatedTreeUuid
        convergence = locationDataCapturer.convergence()
        locationDataCapturer.turnOffTreeCaptureMode()
        isNextButtonActive = true
    }

    fun newTreePhotoCaptured() {
        if (FeatureFlags.TREE_DBH_FEATURE_ENABLED) {
            return
        }
        newTreeUuid = locationDataCapturer.generatedTreeUuid
        convergence = locationDataCapturer.convergence()
        locationDataCapturer.turnOffTreeCaptureMode()
    }

    fun newTreeCaptureCancelled() {
        newTreeUuid = null
        locationDataCapturer.turnOffTreeCaptureMode()
    }

    companion object {
        const val FOCUS_THRESHOLD = 700.0
    }
}
