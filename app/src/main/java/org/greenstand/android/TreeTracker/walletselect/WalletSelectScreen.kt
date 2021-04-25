package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.LanguageButton

@Composable
fun WalletSelectScreen(
    planterInfoId: Long,
    viewModel: WalletSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val state by viewModel.state.observeAsState(initial = WalletSelectState())

    LaunchedEffect(true) {
        viewModel.loadPlanter(planterInfoId)
    }

    val navController = LocalNavHostController.current

    Scaffold(
        topBar = {
            ActionBar(
                rightAction = { LanguageButton() },
                centerAction = {
                    Text("Treetracker", modifier = Modifier.align(Alignment.Center))
                },
                leftAction = {
                    // selfie image
                }
            )
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedPlanter != null
                    ) {
                    }
                },
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                }
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.currentPlanter?.let { currentPlanter ->
                item {
                    Text("You")
                    WalletItem(currentPlanter, state.selectedPlanter == currentPlanter) {
                        viewModel.selectPlanter(it)
                    }
                    Text("Them")
                }
            }
            state.alternatePlanters?.let { alternatePlanters ->
                items(alternatePlanters) { planter ->
                    WalletItem(planter, state.selectedPlanter == planter) {
                        viewModel.selectPlanter(it)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletItem(planterInfo: PlanterInfoEntity, isSelected: Boolean, onClick: (Long) -> Unit) {
    DepthButton(
        modifier = Modifier
            .padding(16.dp)
            .size(height = 80.dp, width = 156.dp),
        isSelected = isSelected,
        onClick = { onClick(planterInfo.id) }
    ) {
        Column {
            Text(text = planterInfo.firstName)
            Text(text = planterInfo.identifier)
        }
    }
}
