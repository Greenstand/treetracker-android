package org.greenstand.android.TreeTracker.splash

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.koin.core.component.KoinComponent

class SplashScreenPreviewProvider : PreviewParameterProvider<SplashScreenViewModel>, KoinComponent {

    override val values: Sequence<SplashScreenViewModel> = sequenceOf(
        SplashScreenViewModel(
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
            getKoin().get(),
        )
    )

    override val count: Int = values.count()
}
