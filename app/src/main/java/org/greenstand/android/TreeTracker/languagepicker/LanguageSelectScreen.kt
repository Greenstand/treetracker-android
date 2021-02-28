package org.greenstand.android.TreeTracker.languagepicker

import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.Language

@Composable
fun LanguageSelectScreen(onNavNext: () -> Unit, viewModel: LanguagePickerViewModel) {

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