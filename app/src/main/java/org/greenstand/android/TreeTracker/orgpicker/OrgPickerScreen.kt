package org.greenstand.android.TreeTracker.orgpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun OrgPickerScreen(viewModel: OrgPickerViewModel = viewModel(factory = LocalViewModelFactory.current)) {

    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(OrgPickerState())

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = { TopBarTitle() }
            )
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                    ) {
                        navController.popBackStack()
                    }
                }
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Org Selection",
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
            items(state.orgs) { org ->
                OrgItem(
                    org = org,
                    isSelected = org == state.currentOrg,
                    onClick = {
                        viewModel.setOrg(org)
                    }
                )
            }
        }
    }
}

@Composable
fun OrgItem(org: Org, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color = if (isSelected) Color.DarkGray else Color.Gray)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(org.name)
    }
}