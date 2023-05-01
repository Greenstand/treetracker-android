package org.greenstand.android.TreeTracker.devoptions

import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys

sealed interface Config {
    val key: PrefKey
    val name: String
}

data class BooleanConfig(
    override val key: PrefKey,
    override val name: String,
    val defaultValue: Boolean,
) : Config

data class IntConfig(
    override val key: PrefKey,
    override val name: String,
    val defaultValue: Int,
) : Config

data class FloatConfig(
    override val key: PrefKey,
    override val name: String,
    val defaultValue: Float,
) : Config

object ConfigKeys {

    val FORCE_IMAGE_SIZE = BooleanConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("force-image-size"),
        name = "Force Image Size",
        defaultValue = false,
    )

    val IMAGE_CAPTURE_HEIGHT = IntConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("image-capture-height"),
        name = "Image Capture Height",
        defaultValue = 1920,
    )

    val CONVERGENCE_DATA_SIZE = IntConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("convergence-data-size"),
        name = "Convergence Data Size",
        defaultValue = 5,
    )

    val CONVERGENCE_TIMEOUT = IntConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("convergence-timeout"),
        name = "Convergence Timeout",
        defaultValue = 20000,
    )

    val LON_STD_DEV_THRESHOLD = FloatConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("lon-std-dev-threshold"),
        name = "Lon Std Dev Threshold",
        defaultValue = 0.00001F,
    )

    val LAT_STD_DEV_THRESHOLD = FloatConfig(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("lat-std-dev-threshold"),
        name = "Lat Std Dev Threshold",
        defaultValue = 0.00001F,
    )

    val configList: List<Config> = listOf(
        FORCE_IMAGE_SIZE,
        IMAGE_CAPTURE_HEIGHT,
        CONVERGENCE_TIMEOUT,
        CONVERGENCE_DATA_SIZE,
        LAT_STD_DEV_THRESHOLD,
        LON_STD_DEV_THRESHOLD,
    )
}