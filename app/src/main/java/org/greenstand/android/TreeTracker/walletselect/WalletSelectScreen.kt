package org.greenstand.android.TreeTracker.walletselect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TextButton

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
                    TextButton(
                        enabled = state.isWalletSelected,
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.next,
                        onClick = { }
                    )
                },
                leftAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.back,
                        onClick = { navController.popBackStack() },
                    )
                }
            )
        },
    ) {
        LazyColumn {
            state.currentPlanter?.let { planter ->
                item {
                    Text("You")
                    WalletItem(planter) {
                    }
                    Text("Them")
                }
            }
            state.alternatePlanters?.let { alternatePlanters ->
                items(alternatePlanters) { planter ->
                    WalletItem(planter) {
                    }
                }
            }
        }
    }
}

@Composable
fun WalletItem(planterInfo: PlanterInfoEntity, onClick: (Long) -> Unit) {
    Text(
        text = planterInfo.identifier,
        Modifier
            .clickable {
                onClick(planterInfo.id)
            }
            .padding(16.dp)
    )
}