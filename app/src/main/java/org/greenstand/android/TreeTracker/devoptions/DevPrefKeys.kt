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

    val configList: List<Config> = listOf(
        FORCE_IMAGE_SIZE,
        IMAGE_CAPTURE_HEIGHT,
    )
}