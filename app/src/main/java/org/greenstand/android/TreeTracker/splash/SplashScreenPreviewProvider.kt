package org.greenstand.android.TreeTracker.splash

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.utilities.PreviewUtils

class SplashScreenPreviewProvider : PreviewParameterProvider<SplashScreenViewModel> {

    override val values: Sequence<SplashScreenViewModel> = sequenceOf(
        SplashScreenViewModel(
            PreferencesMigrator(
                PreviewUtils.previewSharedPrefs,
                preferences = Preferences(PreviewUtils.previewSharedPrefs)
            )
        )
    )

    override val count: Int = values.count()
}
