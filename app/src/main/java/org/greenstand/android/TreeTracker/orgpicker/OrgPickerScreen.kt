package org.greenstand.android.TreeTracker.orgpicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrgPickerScreen(viewModel: OrgPickerViewModel = viewModel(factory = LocalViewModelFactory.current)) {

    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(OrgPickerState())

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.select_organization).uppercase(),
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.Center),
                )
            }
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
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.Center
        ) {
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
    DepthButton(
        colors = AppButtonColors.ProgressGreen,
        onClick = onClick,
        isSelected = isSelected,
        modifier = Modifier
            .padding(16.dp)
            .size(height = 80.dp, width = 156.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = org.name.uppercase(),
            fontWeight = FontWeight.Bold,
            color = CustomTheme.textColors.darkText,
            style = CustomTheme.typography.regular
        )
    }
}