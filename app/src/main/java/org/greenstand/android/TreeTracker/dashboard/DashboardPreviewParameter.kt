package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class DashboardPreviewParameter : PreviewParameterProvider<DashboardViewModel> {

    override val values: Sequence<DashboardViewModel> = sequenceOf(
        DashboardViewModel()
    )

    override val count: Int = values.count()
}
