package org.greenstand.android.TreeTracker.orgpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.greenstand.android.TreeTracker.models.Org

@Composable
fun OrgPickerScreen(viewModel: OrgPickerViewModel, navController: NavController) {

    val state by viewModel.state.observeAsState(OrgPickerState())

    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        viewModel.uiEvents.observe(lifecycle) {
            when(it) {
                OrgPickerUIEvents.Exit -> navController.popBackStack()
            }
        }
    }

    Scaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Org Picker",
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