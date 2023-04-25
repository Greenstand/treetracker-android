package org.greenstand.android.TreeTracker.devoptions

import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys

data class Config(
    val key: PrefKey,
    val name: String,
    val defaultValue: Boolean,
)

object DevConfig {

    val FORCE_IMAGE_SIZE = Config(
        key = PrefKeys.SYSTEM_SETTINGS + PrefKey("force-image-size"),
        name = "Force Image Size",
        defaultValue = false,
    )

    val configList: List<Config> = listOf(
        FORCE_IMAGE_SIZE,
    )
}