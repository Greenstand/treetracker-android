package org.greenstand.android.TreeTracker.signup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class SignupViewPreviewProvider : PreviewParameterProvider<SignupViewModel> {

    override val values: Sequence<SignupViewModel> = sequenceOf(
        SignupViewModel()
    )

    override val count: Int = values.count()
}
