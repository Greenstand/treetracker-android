package org.greenstand.android.TreeTracker.languagepicker

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun LanguageSelectScreen(
    isFromTopBar: Boolean,
    viewModel: LanguagePickerViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val currentLanguage by viewModel.currentLanguage.observeAsState()
    val navController = LocalNavHostController.current
    val activity = LocalContext.current as Activity

    Scaffold(
        topBar = {
            LanguageTopBar()
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    ArrowButton(isLeft = false) {
                        if (isFromTopBar) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(NavRoute.SignupFlow.route)
                        }
                        viewModel.refreshAppLanguage(activity)
                    }
                }
            )
        },
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            items(Language.values()) { language ->
                LanguageButton(
                    text = language.toString(),
                    isSelected = language == currentLanguage,
                ) {
                    viewModel.setLanguage(language)
                }
            }
        }
    }
}

@Composable
fun LanguageTopBar() {
    ActionBar(
        centerAction = { TopBarTitle() }
    )
}

@Composable
fun LanguageButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    DepthButton(
        onClick = onClick,
        isSelected = isSelected,
        modifier = Modifier
            .padding(16.dp)
            .size(height = 80.dp, width = 156.dp)
    ) {
        Text(text)
    }
}

@Preview
@Composable
fun LanguageSelectScreen_Preview(
    @PreviewParameter(LanguagePickerPreviewProvider::class) viewModel: LanguagePickerViewModel,
) {
    LanguageSelectScreen(
        true,
        viewModel
    )
}
