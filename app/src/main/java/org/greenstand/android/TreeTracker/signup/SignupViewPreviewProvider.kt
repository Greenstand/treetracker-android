package org.greenstand.android.TreeTracker.signup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class SignupViewPreviewProvider : PreviewParameterProvider<SignupViewModel> {

    override val values: Sequence<SignupViewModel> = sequenceOf(
//        SignupViewModel(
//            Users(
//                LocationUpdateManager()
//            )
//        )
    )

    override val count: Int = values.count()
}
