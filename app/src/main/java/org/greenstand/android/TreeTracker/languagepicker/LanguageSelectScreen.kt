package org.greenstand.android.TreeTracker.languagepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.Language

@Composable
fun LanguageSelectScreen(
    onNavNext: () -> Unit,
    viewModel: LanguagePickerViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val currentLanguage by viewModel.currentLanguage.observeAsState()

    Scaffold(
        bottomBar = {
            Button(
                onClick = onNavNext,
                modifier = Modifier.padding(10.dp)
            ) {
                Text("Next")
            }
        },
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Current Language: $currentLanguage")
            }
            items(Language.values()) { language ->
                Text(
                    text = language.toString(),
                    modifier = Modifier
                        .clickable {
                            viewModel.setLanguage(language)
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun LanguageSelectScreen_Preview(
    @PreviewParameter(LanguagePickerPreviewProvider::class) viewModel: LanguagePickerViewModel
) {
    LanguageSelectScreen(
        onNavNext = { },
        viewModel
    )
}
