package org.greenstand.android.TreeTracker.theme.title

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

@Composable
fun BoxScope.TopBarTitle() {
    val viewModel: TopBarViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by viewModel.state.observeAsState(TopBarState())
    Image(
        painter = painterResource(id = state.icon),
        contentDescription = "Treetracker/Organization icon",
        modifier = Modifier
            .height(100.dp)
            .width(100.dp)
            .align(Alignment.Center)
            .padding(all = 15.dp)
    )
}