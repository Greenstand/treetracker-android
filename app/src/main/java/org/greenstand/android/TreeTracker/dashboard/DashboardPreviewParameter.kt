package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DashboardPreviewParameter : PreviewParameterProvider<DashboardViewModel>, KoinComponent {

    override val values: Sequence<DashboardViewModel> = sequenceOf(
        DashboardViewModel(get(), get(), get(), get(), get(), get(), get())
    )

    override val count: Int = values.count()
}
