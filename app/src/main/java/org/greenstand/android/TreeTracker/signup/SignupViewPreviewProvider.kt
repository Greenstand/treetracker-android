package org.greenstand.android.TreeTracker.signup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.KoinComponent

class SignupViewPreviewProvider : PreviewParameterProvider<SignupViewModel> {

    override val values: Sequence<SignupViewModel> = sequenceOf(
        //SignupViewModel(KoinApplication.)
    )

    override val count: Int = values.count()
}
