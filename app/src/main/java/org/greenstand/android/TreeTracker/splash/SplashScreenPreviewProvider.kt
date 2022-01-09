package org.greenstand.android.TreeTracker.splash

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.utilities.PreviewUtils
import org.koin.core.KoinComponent

class SplashScreenPreviewProvider : PreviewParameterProvider<SplashScreenViewModel>, KoinComponent {

    override val values: Sequence<SplashScreenViewModel> = sequenceOf(
        SplashScreenViewModel(
            PreferencesMigrator(
                PreviewUtils.previewSharedPrefs,
                preferences = Preferences(PreviewUtils.previewSharedPrefs)
            ),
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
        )
    )

    override val count: Int = values.count()
}
